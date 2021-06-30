package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.openapi.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.jetbrains.rd.util.*

class BundleManagementService : BackgroundResolvable {
    companion object {
        fun getInstance(project: Project): BundleManagementService =
            project.getService(BundleManagementService::class.java)
    }

    val bundles = hashMapOf<String, BundleDefinition>()
    val libReExportRequiredSymbolName = hashMapOf<String, LinkedHashSet<String>>()
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

        val definitionService = TargetDefinitionService.getInstance(project)
        val bundleVersionSelection = definitionService.bundleVersionSelection
        val localBundles = definitionService.locations.flatMap { it.bundles }

        val bundleStep = 0.45 / (localBundles.size + 1)
        val name2Source = hashMapOf<String, BundleDefinition>()
        localBundles.forEach { bundle ->
            indicator.checkCanceled()
            indicator.text2 = "Resolving bundle ${bundle.file}"

            bundle.manifest?.also { manifest ->
                val eclipseSourceBundle = manifest.eclipseSourceBundle
                if (eclipseSourceBundle != null) {
                    val symbolName = eclipseSourceBundle.key
                    if (bundleVersionSelection["$symbolName$BundleSymbolNameSourcePostFix"] == manifest.bundleVersion?.toString()) {
                        name2Source[symbolName] = bundle
                    }
                } else if (bundleVersionSelection[bundle.bundleSymbolicName] == manifest.bundleVersion?.toString()) {
                    bundles[bundle.bundleSymbolicName] = bundle
                    bundle.delegateClassPathFile.values.map { it.presentableUrl }
                        .forEach { jarPathInnerBundle[it] = bundle }
                }
            }
            indicator.fraction += bundleStep
        }

        val sourceStep = 0.45 / (name2Source.size + 1)
        name2Source.forEach { (symbolName, source) ->
            indicator.checkCanceled()
            indicator.text2 = "Resolving source ${source.file}"

            val bundle = bundles[symbolName]
            if (bundle != null) {
                bundle.sourceBundle = source
            } else {
                bundles["$symbolName$BundleSymbolNameSourcePostFix"] = source.apply { sourceBundle = this }
            }

            indicator.fraction += sourceStep
        }

        indicator.checkCanceled()
        indicator.text2 = "Resolving dependency tree"
        indicator.fraction = 0.9

        bundles.mapValues { (_, definition) ->
            definition.manifest?.reExportRequiredBundleSymbolNames ?: emptySet()
        }.also { l1ReExport ->
            l1ReExport.forEach { (symbolName, exported) ->
                val export = linkedSetOf<String>()
                libReExportRequiredSymbolName[symbolName] = export

                exported.forEach {
                    export += it
                    fillDependencies(symbolName, export, setOf(it), l1ReExport)
                }
            }
        }

        indicator.fraction = 1.0
    }

    override fun onFinished(project: Project) {
        ExtensionPointManagementService.getInstance(project).backgroundResolve(project, onFinished = {
            object : BackgroundResolvable {
                override fun resolve(project: Project, indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false
                    indicator.fraction = 0.0

                    indicator.checkCanceled()
                    indicator.text = "Rebuild project settings"

                    indicator.text2 = "Clear bundle cache"
                    BundleManifestCacheService.getInstance(project).clearCache()
                    indicator.fraction = 0.25

                    indicator.text2 = "Resolve project library"
                    PdeLibraryResolverRegistry.instance.resolveProject(project, indicator)
                    indicator.fraction = 0.5

                    indicator.text2 = "Reset module settings"
                    val allPDEModules = project.allPDEModules()

                    val step = 0.5 / (allPDEModules.size + 1)
                    allPDEModules.forEach {
                        indicator.checkCanceled()

                        ModuleHelper.resetCompileOutputPath(it)
                        ModuleHelper.resetCompileArtifact(it)
                        PdeLibraryResolverRegistry.instance.resolveModule(it, indicator)

                        indicator.fraction += step
                    }
                    indicator.fraction = 1.0
                }
            }.backgroundResolve(project)
        })
    }

    private tailrec fun fillDependencies(
        symbolName: String, reExport: HashSet<String>, next: Set<String>, libPair: Map<String, Set<String>>
    ) {
        val nextSet = next.filterNot { it == symbolName }.mapNotNull { libPair[it] }.flatten().toSet()
        if (reExport.addAll(nextSet)) fillDependencies(symbolName, reExport, nextSet, libPair)
    }
}
