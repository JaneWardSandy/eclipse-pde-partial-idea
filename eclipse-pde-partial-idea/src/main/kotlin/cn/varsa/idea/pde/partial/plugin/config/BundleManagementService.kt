package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.listener.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.components.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*

class BundleManagementService(private val project: Project) : TargetDefinitionChangeListener {
    companion object {
        fun getInstance(project: Project): BundleManagementService =
            ServiceManager.getService(project, BundleManagementService::class.java)
    }

    val bundles = hashMapOf<String, BundleDefinition>()

    private fun clear() {
        bundles.clear()
    }

    override fun locationsChanged(project: Project, locations: List<TargetLocationDefinition>) {
        clear()

        object : BackgroundResolvable {
            override fun resolve(project: Project, indicator: ProgressIndicator) {
                locations.flatMap { it.bundles }.forEach { bundle ->
                    bundle.manifest?.also { manifest ->
                        val eclipseSourceBundle = manifest.eclipseSourceBundle
                        if (eclipseSourceBundle != null) {
                            bundles[eclipseSourceBundle.key]?.takeIf { it.manifest?.bundleVersion == manifest.bundleVersion }
                                ?.takeIf { it.sourceBundle == null }?.apply { sourceBundle = bundle }
                        } else {
                            bundles.computeIfAbsent(bundle.bundleSymbolicName) { bundle }
                        }
                    }
                }
            }

            override fun onFinished() {
                val cacheService = BundleManifestCacheService.getInstance(project)

                cacheService.clearCache()
                ModuleHelper.resetLibrary(project)
                cacheService.buildCache()

                project.allPDEModules().forEach {
                    ModuleHelper.resetCompileOutputPath(it)
                    ModuleHelper.resetCompileArtifact(it)
                    ModuleHelper.resetLibrary(it)
                }
            }
        }.backgroundResolve(project)
    }
}
