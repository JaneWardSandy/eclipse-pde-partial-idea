package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.service.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.debugger.engine.*
import com.intellij.debugger.impl.*
import com.intellij.debugger.settings.*
import com.intellij.diagnostic.logging.*
import com.intellij.execution.*
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.impl.*
import com.intellij.execution.process.*
import com.intellij.execution.runners.*
import com.intellij.execution.util.*
import com.intellij.openapi.options.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.util.*
import com.intellij.util.execution.*
import org.jdom.*
import java.rmi.registry.*
import java.util.concurrent.*

class PDETargetRemoteRunConfiguration(
    project: Project, factory: ConfigurationFactory, name: String
) : LocatableConfigurationBase<Element>(project, factory, name), RunConfigurationWithSuppressedDefaultRunAction,
    RemoteRunProfile, ModuleRunConfiguration {
    private val target by lazy { TargetDefinitionService.getInstance(project) }
    private val compiler by lazy { CompilerProjectExtension.getInstance(project) }

    // FIXME: 2021/4/27 Default application
    var product = "com.teamcenter.rac.aifrcp.product"
    var application = "com.teamcenter.rac.aifrcp.application"

    var remoteHost = "localhost"

    var rmiPort = 7995
    var rmiName = "WishesService"
    var remotePort = 5005
    var jdkVersion = PDETargetRemoteRunConfigurationEditor.JDKVersionItem.JDK5to8

    var listeningTeardown = false
    var cleanRuntimeDir = false

    var vmParameters = ""
    var programParameters = ""
    val envVariables = mutableMapOf<String, String>()
    var passParentEnvs = true

    init {
        addLogFile("${project.presentableUrl}/out/log/partial.log", "Partial log", true)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        SettingsEditorGroup<PDETargetRemoteRunConfiguration>().apply {
            addEditor(message("run.remote.config.tab.wishes.title"), PDETargetRemoteRunConfigurationEditor())
            addEditor(message("run.remote.config.tab.logs.title"), LogConfigurationPanel())
        }

    override fun checkConfiguration() {
        super<LocatableConfigurationBase>.checkConfiguration()

        if (compiler == null) throw RuntimeConfigurationWarning(message("run.remote.config.noCompiler", project.name))
        if (product.isBlank() || application.isBlank()) throw RuntimeConfigurationWarning(message("run.remote.config.noTargetApplication"))
        if (remoteHost.isBlank() || rmiName.isBlank()) throw RuntimeConfigurationWarning(message("run.remote.config.noRMI"))
    }

    override fun writeExternal(element: Element) {
        logFiles.firstOrNull { it.name == "Partial log" }?.pathPattern = "${project.presentableUrl}/out/log/partial.log"
        super<LocatableConfigurationBase>.writeExternal(element)

        element.getOrCreate("portal").apply {
            setAttribute("product", product)
            setAttribute("application", application)
        }
        element.getOrCreate("remote").apply {
            setAttribute("host", remoteHost)

            setAttribute("rmiPort", rmiPort.toString())
            setAttribute("rmiName", rmiName)

            setAttribute("remotePort", remotePort.toString())
            setAttribute("jdkVersion", jdkVersion.toString())

            setAttribute("listeningTeardown", listeningTeardown.toString())
            setAttribute("cleanRuntimeDir", cleanRuntimeDir.toString())
        }
        element.getOrCreate("parameter").apply {
            setAttribute("vmParameters", vmParameters)
            setAttribute("programParameters", programParameters)
            setAttribute("passParentEnvs", passParentEnvs.toString())

            getOrCreate("envVariables").apply {
                envVariables.forEach { (key, value) ->
                    getOrCreate("option").apply {
                        setAttribute("name", key)
                        setAttribute("value", value)
                    }
                }
            }
        }
    }

    override fun readExternal(element: Element) {
        super<LocatableConfigurationBase>.readExternal(element)

        element.getChild("portal")?.also {
            product = it.getAttributeValue("product", product)
            application = it.getAttributeValue("application", application)
        }
        element.getChild("remote")?.also { remote ->
            remoteHost = remote.getAttributeValue("host", remoteHost)

            rmiPort = remote.getAttributeValue("rmiPort", rmiPort.toString()).toIntOrNull() ?: rmiPort
            rmiName = remote.getAttributeValue("rmiName", rmiName)

            remotePort = remote.getAttributeValue("remotePort", remotePort.toString()).toIntOrNull() ?: remotePort
            jdkVersion = remote.getAttributeValue("jdkVersion", "").run {
                PDETargetRemoteRunConfigurationEditor.JDKVersionItem.values().firstOrNull { it.toString() == this }
            } ?: PDETargetRemoteRunConfigurationEditor.JDKVersionItem.JDK5to8

            listeningTeardown = remote.getAttributeValue("listeningTeardown", listeningTeardown.toString()).toBoolean()
            cleanRuntimeDir = remote.getAttributeValue("cleanRuntimeDir", cleanRuntimeDir.toString()).toBoolean()
        }
        element.getChild("parameter")?.also { parameter ->
            vmParameters = parameter.getAttributeValue("vmParameters", vmParameters)
            programParameters = parameter.getAttributeValue("programParameters", programParameters)
            passParentEnvs = parameter.getAttributeValue("passParentEnvs", passParentEnvs.toString()).toBoolean()

            envVariables.clear()
            parameter.getChild("envVariables")?.getChildren("option")?.map {
                it.getAttributeValue("name") to it.getAttributeValue("value")
            }?.filter { !it.first.isNullOrBlank() && it.second != null }?.also { envVariables += it }
        }

        logFiles.firstOrNull { it.name == "Partial log" }?.pathPattern = "${project.presentableUrl}/out/log/partial.log"
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val debuggerSettings = environment.runnerSettings as GenericDebuggerRunnerSettings?
        if (debuggerSettings != null) {
            // sync self state with execution environment's state if available
            debuggerSettings.LOCAL = false
            debuggerSettings.debugPort = remotePort.toString()
            debuggerSettings.transport = DebuggerSettings.SOCKET_TRANSPORT
        }

        return PDERemoteState()
    }

    private inner class PDERemoteState : RemoteState {
        private val connection = RemoteConnection(true, remoteHost, remotePort.toString(), false)

        override fun getRemoteConnection(): RemoteConnection = connection
        override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
            val wishesService = LocateRegistry.getRegistry(remoteHost, rmiPort).lookup(rmiName) as WishesService
            if (cleanRuntimeDir) {
                wishesService.destroy()
                wishesService.clean()
            }

            if (!wishesService.isPortalLaunched()) makeWishesReady(wishesService)
            wishesService.launch()

            try {
                FutureTask {
                    while (true) {
                        if (wishesService.isJDWPRunning()) break
                        Thread.sleep(100)
                    }
                }.get(1, TimeUnit.SECONDS)
            } catch (e: Exception) {
            }
            if (!wishesService.isJDWPRunning()) throw IllegalStateException("JDWP not running and connection time out")


            // Remote debug process
            val consoleView = ConsoleViewImpl(project, false)
            val process = RemoteDebugProcessHandler(project, false)

            if (listeningTeardown) {
                process.addProcessListener(object : ProcessAdapter() {
                    override fun processTerminated(event: ProcessEvent) {
                        try {
                            (LocateRegistry.getRegistry(remoteHost, rmiPort).lookup(rmiName) as WishesService).destroy()
                        } catch (e: Exception) {
                        }
                    }
                })
            }
            consoleView.attachToProcess(process)
            return DefaultExecutionResult(consoleView, process)
        }

        private fun makeWishesReady(wishesService: WishesService) {
            val devModels = project.allPDEModules().mapNotNull { PDEFacet.getInstance(it) }.map(PDEFacet::toDevModule)

            wishesService.setupTargetProgram(product, application)
            wishesService.setupStartupLevels(target.startupLevels)
            wishesService.setupModules(devModels)

            val parameters = JavaCommandParameters().apply {
                mainClass = "org.eclipse.equinox.launcher.Main"
                passParentEnvs = this@PDETargetRemoteRunConfiguration.passParentEnvs

                env.putAll(envVariables)

                vmParameters.add(jdkVersion.getLaunchCommandLine(connection))
                vmParameters.addAll(ProgramParametersConfigurator.expandMacrosAndParseParameters(this@PDETargetRemoteRunConfiguration.vmParameters))

                programParameters.addAll("-name", "Teamcenter")
                programParameters.addAll("-showsplash", "600")

                if (product.isNotBlank()) {
                    programParameters.addAll("-product", product)
                } else if (application.isNotBlank()) {
                    programParameters.addAll("-application", application)
                }

                programParameters.addAll(*ParametersListUtil.parseToArray(this@PDETargetRemoteRunConfiguration.programParameters))
                programParameters.add("-consoleLog")
            }
            wishesService.generateData(parameters)
        }
    }
}
