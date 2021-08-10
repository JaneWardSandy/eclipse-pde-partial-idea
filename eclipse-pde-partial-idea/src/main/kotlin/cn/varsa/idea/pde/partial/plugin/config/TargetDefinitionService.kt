package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.listener.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.components.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.util.xmlb.*
import com.intellij.util.xmlb.annotations.*
import java.io.*

@State(name = "TcRacTargetDefinitions", storages = [Storage("eclipse-partial.xml")])
class TargetDefinitionService : PersistentStateComponent<TargetDefinitionService>, BackgroundResolvable {
    @XCollection(elementName = "locations", style = XCollection.Style.v2) val locations =
        mutableListOf<TargetLocationDefinition>()

    @Attribute var launcherJar: String? = null
    @Attribute var launcher: String? = null

    @XMap(entryTagName = "bundleLevel", keyAttributeName = "bundleSymbolicName", valueAttributeName = "startupLevel")
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

    override fun resolve(project: Project, indicator: ProgressIndicator) {
        indicator.checkCanceled()
        indicator.isIndeterminate = false
        indicator.fraction = 0.0

        val step = 1 / (locations.size + 1)
        locations.forEach {
            indicator.checkCanceled()
            it.resolve(project, indicator)
            indicator.fraction += step
        }

        val sourceVersions =
            locations.flatMap { it.bundles }.groupBy { it.manifest?.eclipseSourceBundle?.key }.filterKeys { it != null }
                .mapValues { it.value.distinct().toHashSet() }
        locations.forEach { location ->
            location.bundles.forEach { bundle ->
                bundle.sourceBundle =
                    sourceVersions[bundle.bundleSymbolicName]?.firstOrNull { location.bundleVersionSelection[bundle.canonicalName] == it.bundleVersion.toString() }
            }
        }

        indicator.fraction = 1.0
    }

    override fun onFinished(project: Project) {
        TargetDefinitionChangeListener.notifyLocationsChanged(project)
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

    @Attribute var alias: String? = null

    @XCollection(elementName = "unSelectedBundles") val bundleUnSelected = mutableListOf<String>()

    @XMap(entryTagName = "sourceVersion", keyAttributeName = "canonicalName", valueAttributeName = "version")
    val bundleVersionSelection = hashMapOf<String, String>()

    private val _bundles = mutableListOf<BundleDefinition>()

    val identifier: String get() = alias?.takeIf(String::isNotBlank) ?: location
    val bundles: List<BundleDefinition> = _bundles

    init {
        _location.takeIf(String::isNotBlank)?.also { location = it }
    }

    override fun resolve(project: Project, indicator: ProgressIndicator) {
        val scope = DependencyScope.values().firstOrNull { it.displayName == dependency } ?: DependencyScope.COMPILE
        _bundles.clear()

        indicator.text = "Resolving location $location"
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
            indicator.text2 = "Resolving file $file"

            val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return@forEach
            if (file.isFile && file.extension.equalAny("jar", "aar", "war", ignoreCase = true)) {
                if (file.name.startsWith("org.eclipse.equinox.launcher_")) {
                    launcherJar = file.canonicalPath
                }

                JarFileSystem.getInstance().getJarRootForLocalFile(virtualFile)?.also { jarFile ->
                    _bundles += BundleDefinition(jarFile, file, this, project, scope)
                }
            } else if (file.isDirectory && File(file, ManifestPath).exists()) {
                _bundles += BundleDefinition(virtualFile, file, this, project, scope)
            }
        }
    }

    override fun toString(): String =
        "$location [${bundles.size} bundles available, launcher: $launcher, launcher jar: $launcherJar]"
}
