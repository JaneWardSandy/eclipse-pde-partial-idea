package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.components.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.*
import com.intellij.util.xmlb.*
import com.intellij.util.xmlb.annotations.*
import java.io.*

@State(name = "TcRacTargetDefinitions", storages = [Storage("eclipse-partial.xml")])
class TargetDefinitionService : PersistentStateComponent<TargetDefinitionService>, BackgroundResolvable {
    @XCollection(elementName = "locations", style = XCollection.Style.v2) val locations =
        mutableListOf<TargetLocationDefinition>()

    @Attribute var launcherJar: String? = null
    @Attribute var launcher: String? = null

    @XMap(entryTagName = "BundleLevel", keyAttributeName = "bundleSymbolicName", valueAttributeName = "startupLevel")
    val startupLevels = hashMapOf(
        "org.eclipse.core.runtime" to 4,
        "org.eclipse.equinox.common" to 2,
        "org.eclipse.equinox.ds" to 1,
        "org.eclipse.equinox.simpleconfigurator" to 1,
        "org.eclipse.equinox.event" to 2,
        "org.eclipse.equinox.p2.reconciler.dropins" to 4,
        "org.eclipse.osgi" to -1,
        "org.eclipse.m2e.logback.configuration" to 4,
        "org.apache.felix.scr" to 2,
        "org.apache.felix.gogo.command" to 4,
        "org.apache.felix.gogo.runtime" to 4,
        "org.apache.felix.gogo.shell" to 4,
        "org.eclipse.equinox.console" to 4
    )

    companion object {
        fun getInstance(project: Project): TargetDefinitionService =
            project.getService(TargetDefinitionService::class.java)
    }

    override fun resolve(indicator: ProgressIndicator) {
        locations.forEach {
            indicator.checkCanceled()
            it.resolve(indicator)
        }
    }

    override fun getState(): TargetDefinitionService = this
    override fun loadState(state: TargetDefinitionService) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

@Tag("location")
class TargetLocationDefinition(_location: String = "") : BackgroundResolvable {
    @Attribute var location: String = ""
        private set

    @Attribute var launcherJar: String? = null
    @Attribute var launcher: String? = null
    @Attribute var dependency = DependencyScope.COMPILE.displayName

    val bundles = mutableListOf<File>()

    init {
        _location.takeIf(String::isNotBlank)?.also { location = it }
    }

    override fun resolve(indicator: ProgressIndicator) {
        bundles.clear()

        indicator.text2 = "Resolving location $location"
        indicator.checkCanceled()

        val directory = File(location)
        if (!directory.isDirectory) return
        if (!directory.exists()) return

        if (SystemInfo.isMac) {
            File(directory.parentFile, "MacOS/eclipse").takeIf(File::exists)?.also { launcher = it.canonicalPath }
        } else if (SystemInfo.isWindows) {
            arrayOf("Teamcenter.exe", "eclipse.exe").map { File(directory, it) }.firstOrNull(File::exists)
                ?.also { launcher = it.canonicalPath }
        }

        indicator.checkCanceled()

        val pluginsDirectory = File(directory, Plugins).takeIf(File::exists) ?: directory
        pluginsDirectory.listFiles()?.filterNot { it.isHidden }?.forEach { file ->
            indicator.checkCanceled()

            if (file.isFile && file.extension.equalAny("jar", "aar", "war", ignoreCase = true)) {
                bundles += file.canonicalFile
                if (file.name.startsWith("org.eclipse.equinox.launcher_")) {
                    launcherJar = file.canonicalPath
                }
            } else if (file.isDirectory && File(file, ManifestPath).exists()) {
                bundles += file.canonicalFile
            }
        }
    }

    override fun toString(): String =
        "$location [${bundles.size} bundles available, launcher: $launcher, launcher jar: $launcherJar]"
}

interface BackgroundResolvable {
    fun resolve(indicator: ProgressIndicator)

    fun backgroundResolve(
        project: Project,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {},
        onThrowable: (Throwable) -> Unit = { _ -> },
        onFinished: () -> Unit = {},
    ) {
        object : Task.Backgroundable(project, message("config.target.service.resolving"), true, DEAF) {
            override fun run(indicator: ProgressIndicator) {
                indicator.checkCanceled()
                resolve(indicator)
            }

            override fun onSuccess() {
                super.onSuccess()
                onSuccess()
            }

            override fun onCancel() {
                super.onCancel()
                onCancel()
            }

            override fun onThrowable(error: Throwable) {
                super.onThrowable(error)
                onThrowable(error)
            }

            override fun onFinished() {
                super.onFinished()
                onFinished()
            }
        }.setCancelText(message("config.target.service.cancel")).queue()
    }
}
