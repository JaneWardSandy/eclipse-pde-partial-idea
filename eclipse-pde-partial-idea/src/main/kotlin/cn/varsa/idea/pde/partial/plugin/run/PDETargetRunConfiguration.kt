package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.configure.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.service.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.diagnostic.logging.*
import com.intellij.execution.*
import com.intellij.execution.application.*
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.*
import com.intellij.execution.runners.*
import com.intellij.execution.util.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.options.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.search.*
import com.intellij.util.*
import org.jdom.*
import org.jetbrains.kotlin.idea.util.projectStructure.*
import java.io.*
import java.util.*

class PDETargetRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    ApplicationConfiguration(name, project, factory) {
    private val target by lazy { TargetDefinitionService.getInstance(project) }
    private val cache by lazy { BundleManifestCacheService.getInstance(project) }
    private val compiler by lazy { CompilerProjectExtension.getInstance(project) }

    // FIXME: 2021/4/27 Default application
    var product = "com.teamcenter.rac.aifrcp.product"
    var application = "com.teamcenter.rac.aifrcp.application"

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        SettingsEditorGroup<PDETargetRunConfiguration>().apply {
            addEditor(message("run.local.config.tab.configuration.title"), PDETargetRunConfigurationEditor(project))
            JavaRunConfigurationExtensionManager.instance.appendEditors(this@PDETargetRunConfiguration, this)
            addEditor(message("run.local.config.tab.logs.title"), LogConfigurationPanel())
        }

    override fun checkConfiguration() {
        JavaParametersUtil.checkAlternativeJRE(this)
        ProgramParametersUtil.checkWorkingDirectoryExist(this, project, configurationModule.module)
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this)

        if (compiler == null) throw RuntimeConfigurationWarning(message("run.local.config.noCompiler", project.name))
        if (target.launcher == null) throw RuntimeConfigurationWarning(message("run.local.config.noTargetLauncher"))
        if (target.launcherJar == null) throw RuntimeConfigurationWarning(message("run.local.config.noTargetLauncherJar"))
        if (product.isBlank() && application.isBlank()) throw RuntimeConfigurationWarning(message("run.local.config.noTargetApplication"))
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.getOrCreate("partial").apply {
            setAttribute("product", product)
            setAttribute("application", application)
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.getChild("partial")?.also {
            product = it.getAttributeValue("product") ?: ""
            application = it.getAttributeValue("application") ?: ""
        }
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState =
        PDEApplicationCommandLineState(this, env).apply {
            consoleBuilder =
                TextConsoleBuilderFactory.getInstance().createBuilder(project, GlobalSearchScope.allScope(project))
        }

    private inner class PDEApplicationCommandLineState(
        configuration: PDETargetRunConfiguration, environment: ExecutionEnvironment?
    ) : JavaApplicationCommandLineState<PDETargetRunConfiguration>(
        configuration, environment
    ) {
        override fun createJavaParameters(): JavaParameters {
            val parameters: JavaParameters = super.createJavaParameters()

            try {
                if (SystemInfo.isMac) parameters.vmParametersList.add("-XstartOnFirstThread")

                parameters.classPath.clear()
                parameters.classPath.add(target.launcherJar!!)
                parameters.programParametersList.addAll("-launcher", target.launcher!!)
                parameters.programParametersList.addAll("-name", "Teamcenter")
                parameters.programParametersList.addAll("-showsplash", "600")

                if (product.isNotBlank()) {
                    parameters.programParametersList.addAll("-product", product)
                } else if (application.isNotBlank()) {
                    parameters.programParametersList.addAll("-application", application)
                }

                val properties =
                    target.locations.map { File(it.location, "configuration/config.ini") }.firstOrNull(File::exists)
                        ?.inputStream()?.use { Properties().apply { load(it) } } ?: Properties()
                LaunchConfigGenerator.storeConfigIniFile(configServiceDelegate, properties)
                LaunchConfigGenerator.storeDevProperties(configServiceDelegate)
                LaunchConfigGenerator.storeBundleInfo(configServiceDelegate)

                parameters.programParametersList.addAll("-data", configServiceDelegate.dataPath.absolutePath)
                parameters.programParametersList.addAll(
                    "-configuration", configServiceDelegate.configurationDirectory.protocolUrl
                )
                parameters.programParametersList.addAll("-dev", configServiceDelegate.devPropertiesFile.protocolUrl)

                parameters.programParametersList.add("-consoleLog")
            } catch (e: Exception) {
                thisLogger().error(e.message, e)
                throw e
            }

            return parameters
        }
    }

    private val configServiceDelegate = object : ConfigService {
        override val product: String get() = this@PDETargetRunConfiguration.product
        override val application: String get() = this@PDETargetRunConfiguration.application

        override val dataPath: File get() = File(compiler!!.compilerOutputPointer.presentableUrl, "partial-runtime")
        override val installArea: File get() = target.launcher!!.toFile().parentFile
        override val projectDirectory: File get() = project.presentableUrl!!.toFile()

        override val libraries: List<File>
            get() = LibraryTablesRegistrar.getInstance().getLibraryTable(project).run {
                DependencyScope.values().map { it.displayName }.map { getLibraryByName("$ProjectLibraryNamePrefix$it") }
                    .mapNotNull { it?.getFiles(OrderRootType.CLASSES) }.flatMap { it.toList() }
                    .map { it.presentableUrl.toFile() }
            }

        override val devModules: List<DevModule>
            get() = project.allModules().filter { it.isLoaded }.mapNotNull { PDEFacet.getInstance(it) }
                .map(PDEFacet::toDevModule)

        override fun getManifest(jarFileOrDirectory: File): BundleManifest? =
            LocalFileSystem.getInstance().findFileByIoFile(jarFileOrDirectory)?.let { cache.getManifest(it) }

        override fun startUpLevel(bundleSymbolicName: String): Int = target.startupLevels[bundleSymbolicName] ?: 4
        override fun isAutoStartUp(bundleSymbolicName: String): Boolean =
            target.startupLevels.containsKey(bundleSymbolicName)
    }
}
