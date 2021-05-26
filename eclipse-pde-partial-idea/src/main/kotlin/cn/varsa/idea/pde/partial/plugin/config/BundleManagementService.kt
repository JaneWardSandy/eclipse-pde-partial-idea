package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.components.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*

class BundleManagementService : BackgroundResolvable {
    companion object {
        fun getInstance(project: Project): BundleManagementService =
            ServiceManager.getService(project, BundleManagementService::class.java)
    }

    val bundles = hashMapOf<String, BundleDefinition>()
    val libReExportRequiredSymbolName = hashMapOf<String, HashSet<String>>()

    private fun clear() {
        bundles.clear()
        libReExportRequiredSymbolName.clear()
    }

    override fun resolve(project: Project, indicator: ProgressIndicator) {
        clear()
        indicator.checkCanceled()
        indicator.text = "Resolving bundle management"
        indicator.isIndeterminate = false
        indicator.fraction = 0.0

        val bundles = TargetDefinitionService.getInstance(project).locations.flatMap { it.bundles }

        val step = 0.9 / bundles.size
        bundles.forEach { bundle ->
            indicator.checkCanceled()
            indicator.text2 = "Resolving bundle ${bundle.file}"

            bundle.manifest?.also { manifest ->
                val eclipseSourceBundle = manifest.eclipseSourceBundle
                if (eclipseSourceBundle != null) {
                    this.bundles[eclipseSourceBundle.key]?.takeIf { it.manifest?.bundleVersion == manifest.bundleVersion }
                        ?.takeIf { it.sourceBundle == null }?.apply { sourceBundle = bundle }
                } else {
                    this.bundles.computeIfAbsent(bundle.bundleSymbolicName) { bundle }
                }
            }
            indicator.fraction += step
        }

        indicator.checkCanceled()
        indicator.text2 = "Resolving dependency tree"
        indicator.fraction = 0.9

        this.bundles.map { it.key to (it.value.manifest?.reExportRequiredBundleSymbolNames?.toHashSet() ?: hashSetOf()) }
            .toMap().also { libReExportRequiredSymbolName += it }.run {
                forEach { (symbolName, reExport) -> fillDependencies(symbolName, reExport, reExport, this) }
            }
        indicator.fraction = 1.0
    }

    override fun onFinished(project: Project) {
        object : BackgroundResolvable {
            override fun resolve(project: Project, indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                indicator.checkCanceled()
                indicator.text = "Rebuild project settings"

                indicator.text2 = "Clear bundle cache"
                BundleManifestCacheService.getInstance(project).clearCache()
                indicator.fraction = 0.25

                indicator.text2 = "Reset project library"
                ModuleHelper.resetLibrary(project)
                indicator.fraction = 0.5

                indicator.text2 = "Reset module settings"
                val allPDEModules = project.allPDEModules()

                val step = 0.5 / allPDEModules.size
                allPDEModules.forEach {
                    indicator.checkCanceled()

                    ModuleHelper.resetCompileOutputPath(it)
                    ModuleHelper.resetCompileArtifact(it)
                    ModuleHelper.resetLibrary(it)

                    indicator.fraction += step
                }
                indicator.fraction = 1.0
            }
        }.backgroundResolve(project)
    }

    private tailrec fun fillDependencies(
        symbolName: String, reExport: HashSet<String>, next: Set<String>, libPair: Map<String, Set<String>>
    ) {
        val nextSet = next.filterNot { it == symbolName }.mapNotNull { libPair[it] }.flatten().toSet()
        if (reExport.addAll(nextSet)) fillDependencies(symbolName, reExport, nextSet, libPair)
    }
}
