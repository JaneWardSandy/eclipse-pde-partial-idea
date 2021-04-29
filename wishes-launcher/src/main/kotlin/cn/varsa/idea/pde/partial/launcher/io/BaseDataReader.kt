package cn.varsa.idea.pde.partial.launcher.io

import cn.varsa.idea.pde.partial.launcher.support.*
import java.io.*
import java.util.concurrent.*

abstract class BaseDataReader {
    private val logger = thisLogger()

    @Volatile protected var stoped = false
    private val sleepMonitor = Object()

    private var finishedFuture: Future<*>? = null

    protected open fun start() {
        if (finishedFuture == null) {
            finishedFuture = executeOnPooledThread(this::doRun)
        }
    }

    protected abstract fun readAvailable(): Boolean
    protected abstract fun executeOnPooledThread(runnable: Runnable): Future<*>

    protected open fun doRun() {
        try {
            var stopSignalled = false
            while (true) {
                val read = readAvailable()
                if (stopSignalled) break

                stopSignalled = stoped

                if (!stopSignalled) {
                    beforeSleeping(read)
                    synchronized(sleepMonitor) { sleepMonitor.wait(if (read) 1 else 5) }
                }
            }
        } catch (e: IOException) {
            logger.info(e.message, e)
        } catch (e: Exception) {
            logger.error(e.message, e)
        } finally {
            flush()
            try {
                close()
            } catch (e: IOException) {
                logger.error("Can't close stream", e)
            }
        }
    }

    protected open fun flush() {}
    protected open fun beforeSleeping(hasJustReadSomething: Boolean) {}
    protected abstract fun close()

    open fun stop() {
        stoped = true
        synchronized(sleepMonitor) { sleepMonitor.notifyAll() }
    }

    open fun waitFor() {
        try {
            finishedFuture?.get()
        } catch (e: ExecutionException) {
            logger.error(e.message, e)
        }
    }

    open fun waitFor(timeout: Long, unit: TimeUnit) {
        try {
            finishedFuture?.get(timeout, unit)
        } catch (e: ExecutionException) {
            logger.error(e.message, e)
        }
    }
}
