package cn.varsa.idea.pde.partial.launcher.command

import cn.varsa.idea.pde.partial.launcher.io.*
import cn.varsa.idea.pde.partial.launcher.support.*
import java.io.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import java.util.function.*

class ProcessHandler(commandLine: CommandLine) {
    private val logger = thisLogger()

    val process = commandLine.createProcess()
    var exitCode = 0

    private val waitFor = ProcessWaitFor(process, this)
    private val state: AtomicReference<State> = AtomicReference(State.INITIAL)
    private val listeners = CopyOnWriteArrayList<ProcessListener>()

    val processInput: OutputStream
        get() = process.outputStream
    val isStartNotified: Boolean
        get() = state.get() != State.INITIAL
    val isProcessTerminated: Boolean
        get() = state.get() == State.TERMINATED
    val isProcessTerminating: Boolean
        get() = state.get() == State.TERMINATING

    init {
        addProcessListener(object : ProcessListener {
            override fun onProcessStart() {
                try {
                    val stdOutReader =
                        SimpleOutputReader(BaseInputStreamReader(process.inputStream), ProcessOutputType.STDOUT)
                    val stdErrReader =
                        SimpleOutputReader(BaseInputStreamReader(process.errorStream), ProcessOutputType.STDERR)

                    waitFor.setTerminationCallback { exitCode ->
                        try {
                            stdErrReader.stop()
                            stdOutReader.stop()

                            try {
                                stdErrReader.waitFor()
                                stdOutReader.waitFor()
                            } catch (_: InterruptedException) {
                            }
                        } finally {
                            notifyProcessTerminated(exitCode.toInt())
                        }
                    }
                } finally {
                    removeProcessListener(this)
                }
            }
        })

        addProcessListener(object : ProcessListener {
            override fun onProcessTerminated(exitCode: Int) {
                removeProcessListener(this)
                logger.info("Process finished with exit code $exitCode")
            }
        })
    }

    fun addProcessListener(listener: ProcessListener) = listeners.add(listener)
    fun removeProcessListener(listener: ProcessListener) = listeners.remove(listener)

    fun waitFor() = waitFor.waitFor()
    fun waitFor(timeout: Long, unit: TimeUnit): Boolean = waitFor.waitFor(timeout, unit)

    fun executeTask(task: Runnable): Future<*> = processIOExecutor.submit(task)

    fun startNotify() {
        if (state.compareAndSet(State.INITIAL, State.RUNNING)) {
            listeners.forEach(ProcessListener::onProcessStart)
        } else {
            logger.error("startNotify called already")
        }
    }

    fun destroyProcess() {
        if (state.compareAndSet(State.RUNNING, State.TERMINATING)) {
            fireProcessWillTerminate(true)
            try {
                closeStreams()
            } finally {
                process.destroy()
            }
        }
    }

    fun detachProcess() {
        if (state.compareAndSet(State.RUNNING, State.TERMINATING)) {
            fireProcessWillTerminate(false)
            executeTask {
                closeStreams()
                waitFor.detach()
                notifyProcessDetached()
            }
        }
    }

    private fun notifyProcessDetached() = notifyTerminated(0, false)
    private fun notifyProcessTerminated(exitCode: Int) = notifyTerminated(exitCode, true)
    private fun notifyTextAvailable(text: String, type: ProcessOutputType) {
        listeners.forEach { it.onTextAvailable(text, type) }
    }

    private fun notifyTerminated(exitCode: Int, willBeDestroyed: Boolean) {
        if (state.compareAndSet(State.RUNNING, State.TERMINATING)) {
            try {
                fireProcessWillTerminate(willBeDestroyed)
            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        }
        if (state.compareAndSet(State.TERMINATING, State.TERMINATED)) {
            this.exitCode = exitCode
            listeners.forEach { it.onProcessTerminated(exitCode) }
        }
    }

    private fun fireProcessWillTerminate(willBeDestroyed: Boolean) {
        listeners.forEach { it.onProcessWillTerminate(willBeDestroyed) }
    }

    private fun closeStreams() {
        try {
            process.outputStream.close()
        } catch (e: IOException) {
            logger.warn(e.message, e)
        }
    }

    private enum class State {
        INITIAL, RUNNING, TERMINATING, TERMINATED
    }

    companion object {
        val processIOExecutor = ThreadPoolExecutor(1, Int.MAX_VALUE, 1, TimeUnit.MINUTES, SynchronousQueue())
    }

    inner class SimpleOutputReader(reader: Reader, private val type: ProcessOutputType) : BaseOutputReader(reader) {

        init {
            start()
        }

        override fun onTextAvailable(text: String) = notifyTextAvailable(text, type)
        override fun executeOnPooledThread(runnable: Runnable): Future<*> = executeTask(runnable)
    }
}

interface ProcessListener {
    fun onProcessStart() {}
    fun onProcessTerminated(exitCode: Int) {}
    fun onProcessWillTerminate(willBeDestroyed: Boolean) {}
    fun onTextAvailable(text: String, type: ProcessOutputType) {}
}

enum class ProcessOutputType {
    SYSTEM, STDOUT, STDERR;

    fun isStdout() = this == STDOUT
    fun isStderr() = this == STDERR
}

class ProcessWaitFor(process: Process, handler: ProcessHandler) {
    @Volatile private var detached = false
    private val terminationCallback = ArrayBlockingQueue<Consumer<Number>>(1)

    private val waitForThreadFuture: Future<*> = handler.executeTask {
        var exitCode = 0
        try {
            while (!detached) {
                try {
                    exitCode = process.waitFor()
                    break
                } catch (_: InterruptedException) {
                }
            }
        } finally {
            if (!detached) {
                try {
                    terminationCallback.take().accept(exitCode)
                } catch (_: InterruptedException) {
                }
            }
        }
    }

    fun detach() {
        detached = true
        waitForThreadFuture.cancel(true)
    }

    fun setTerminationCallback(r: Consumer<Number>) = terminationCallback.offer(r)

    fun waitFor() {
        try {
            waitForThreadFuture.get()
        } catch (_: CancellationException) {
        }
    }

    fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
        try {
            waitForThreadFuture.get(timeout, unit)
        } catch (_: CancellationException) {
        } catch (_: TimeoutException) {
        }

        return waitForThreadFuture.isDone
    }
}
