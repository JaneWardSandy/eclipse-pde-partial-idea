package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.jetbrains.rd.util.*

class BundleManagementService : BackgroundResolvable {
    companion object {
        fun getInstance(project: Project): BundleManagementService =
            project.getService(BundleManagementService::class.java)
    }

    val bundles = ConcurrentHashMap<String, BundleDefinition>()
    val libReExportRequiredSymbolName = hashMapOf<String, HashSet<String>>()
    val jarPathInnerBundle = hashMapOf<String, BundleDefinition>()

    private fun clear() {
        bundles.clear()
        libReExportRequiredSymbolName.clear()
        jarPathInnerBundle.clear()
    }

    override fun resolve(project: Project, indicator: ProgressIndicator) {
        clear()
        indicator.checkCanceled()
        indicator.text = "Resolving bundle management"
        indicator.isIndeterminate = false
        indicator.fraction = 0.0

        val localBundles = TargetDefinitionService.getInstance(project).locations.flatMap { it.bundles }

        val step = 0.9 / (localBundles.size + 1)
        localBundles.forEach { bundle ->
            indicator.checkCanceled()
            indicator.text2 = "Resolving bundle ${bundle.file}"

            bundle.manifest?.also { manifest ->
                val eclipseSourceBundle = manifest.eclipseSourceBundle
                if (eclipseSourceBundle != null) {
                    bundles[eclipseSourceBundle.key]?.takeIf { it.manifest?.bundleVersion == manifest.bundleVersion }
                        ?.takeIf { it.sourceBundle == null }?.apply { sourceBundle = bundle }
                } else {
                    bundles.computeIfAbsent(bundle.bundleSymbolicName) {
                        bundle.delegateClassPathFile.map { it.presentableUrl }.forEach { jarPathInnerBundle[it] = bundle }
                        bundle
                    }
                }
            }
            indicator.fraction += step
        }

        indicator.checkCanceled()
        indicator.text2 = "Resolving dependency tree"
        indicator.fraction = 0.9

        bundles.map {
            it.key to (it.value.manifest?.reExportRequiredBundleSymbolNames?.toHashSet() ?: hashSetOf())
        }.toMap().also { libReExportRequiredSymbolName += it }.run {
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

                val step = 0.5 / (allPDEModules.size + 1)
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
