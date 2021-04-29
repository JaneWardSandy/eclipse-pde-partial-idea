package cn.varsa.idea.pde.partial.launcher.control

import cn.varsa.idea.pde.partial.common.configure.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.launcher.*
import cn.varsa.idea.pde.partial.launcher.command.*
import tornadofx.*
import java.io.*
import java.util.*

class WishesControl : Controller() {
    private val configControl: ConfigControl by inject()
    private val loggerControl: LoggerControl by inject()

    private val setup = JavaCommandLineSetup()

    var handler: ProcessHandler? = null

    fun generateData(parameters: JavaCommandParameters) {
        parameters.mainClass = "org.eclipse.equinox.launcher.Main"
        parameters.workingDirectory = configControl.runtimeDirectory

        parameters.classPath.add(configControl.launcherJar)
        parameters.programParameters.addAll("-launcher", configControl.launcher)

        parameters.programParameters.addAll("-data", configControl.dataPath.absolutePath)
        parameters.programParameters.addAll("-configuration", configControl.configurationDirectory.protocolUrl)
        parameters.programParameters.addAll("-dev", configControl.devPropertiesFile.protocolUrl)

        setup.setupJavaExePath(configControl.javaExe)
        setup.setupCommandLine(parameters)

        val properties = configControl.launcher.let(::File).let { File(it.parentFile, "configuration/config.ini") }
            .takeIf(File::exists)?.inputStream()?.use { Properties().apply { load(it) } } ?: Properties()
        LaunchConfigGenerator.storeConfigIniFile(configControl, properties)
        LaunchConfigGenerator.storeDevProperties(configControl)
        LaunchConfigGenerator.storeBundleInfo(configControl)
    }

    fun clean() {
        if (configControl.dataPath.exists()) {
            try {
                configControl.dataPath.deleteRecursively()
            } finally {
                configControl.dataPath.delete()
            }
        }
    }

    fun launch() {
        if (configControl.portalRunning) return

        loggerControl.initialAppender()
        handler = ProcessHandler(setup.commandLine).apply {
            addProcessListener(object : ProcessListener {
                override fun onProcessStart() {
                    fire(ProcessStartEvent)
                    runLater { configControl.portalRunning = true }
                }

                override fun onProcessWillTerminate(willBeDestroyed: Boolean) {
                    fire(ProcessWillTerminateEvent(willBeDestroyed))
                }

                override fun onProcessTerminated(exitCode: Int) {
                    fire(ProcessTerminatedEvent(exitCode))
                    removeProcessListener(this)
                    handler = null

                    destroy()
                }

                override fun onTextAvailable(text: String, type: ProcessOutputType) {
                    if (type.isStdout()) loggerControl.logger.info(text)
                    if (type.isStderr()) loggerControl.logger.warn(text)

                    if (text.contains("Listening for transport .+ at address: [\\d]+".toRegex())) {
                        runLater { configControl.jdwpRunning = true }
                    }
                }
            })

            if (isStartNotified.not()) startNotify()
        }
    }

    fun destroy() {
        handler?.apply {
            if (!isProcessTerminated) {
                destroyProcess()
            }
        }
        handler = null
        loggerControl.destroyAppender()

        runLater {
            configControl.portalRunning = false
            configControl.jdwpRunning = false
        }
    }
}
