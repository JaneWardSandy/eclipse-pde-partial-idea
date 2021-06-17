package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import com.intellij.util.*
import org.jetbrains.kotlin.idea.util.*

val Module.moduleRootManager: ModuleRootManager get() = ModuleRootManager.getInstance(this)
fun Module.updateModel(task: Consumer<in ModifiableRootModel>) = ModuleRootModificationUtil.updateModel(this, task)

fun Module.isBundleRequiredOrFromReExport(symbolName: String): Boolean {
    val cacheService = BundleManifestCacheService.getInstance(project)
    val managementService = BundleManagementService.getInstance(project)

    val manifest = cacheService.getManifest(this) ?: return false
    val requiredBundle = manifest.requireBundle?.keys ?: return false

    // Bundle required directly
    requiredBundle.contains(symbolName).ifTrue { return true }

    val allRequiredFromReExport =
        requiredBundle.mapNotNull { managementService.libReExportRequiredSymbolName[it] }.flatten().toSet()

    // Re-export dependency tree can resolve bundle
    allRequiredFromReExport.contains(symbolName).ifTrue { return true }

    // Re-export bundle contain module, it need calc again
    val modulesManifest =
        project.allPDEModules().filterNot { it == this }.mapNotNull(cacheService::getManifest).toHashSet()

    modulesManifest.filter {
        it.bundleSymbolicName?.key?.run { requiredBundle.contains(this) || allRequiredFromReExport.contains(this) } == true
    }.any { isBundleFromReExportOnly(it, symbolName, cacheService, managementService, modulesManifest) }
        .ifTrue { return true }

    return false
}

private fun isBundleFromReExportOnly(
    manifest: BundleManifest,
    symbolName: String,
    cacheService: BundleManifestCacheService,
    managementService: BundleManagementService,
    modulesManifest: HashSet<BundleManifest>
): Boolean {
    // Re-export directly
    manifest.reExportRequiredBundleSymbolNames.contains(symbolName).ifTrue { return true }

    val allReExport =
        manifest.reExportRequiredBundleSymbolNames.mapNotNull { managementService.libReExportRequiredSymbolName[it] }
            .flatten().toSet()

    // Re-export dependency tree can resolve bundle
    allReExport.contains(symbolName).ifTrue { return true }

    // Dependency tree contains module, it need calc again, and remove it from module set to not calc again and again and again
    return modulesManifest.filter { allReExport.contains(it.bundleSymbolicName?.key) }.also { modulesManifest -= it }
        .any { isBundleFromReExportOnly(it, symbolName, cacheService, managementService, modulesManifest) }
}

val Module.bundleRequiredOrFromReExportOrderedList: LinkedHashSet<String>
    get() {
        val cacheService = BundleManifestCacheService.getInstance(project)
        val managementService = BundleManagementService.getInstance(project)

        val result = linkedSetOf<String>()

        val manifest = cacheService.getManifest(this) ?: return result
        val requiredBundles = manifest.requireBundle?.keys ?: return result

        val modulesManifest = project.allPDEModules().filterNot { it == this }.mapNotNull(cacheService::getManifest)
            .associateBy { it.bundleSymbolicName?.key }.toMutableMap()

        fun bundleFromReExportOrderedListTo(manifest: BundleManifest) {
            manifest.reExportRequiredBundleSymbolNames.forEach { exportBundle ->
                result += exportBundle
                managementService.libReExportRequiredSymbolName[exportBundle]?.forEach { reExportBundle ->
                    result += reExportBundle
                    modulesManifest.remove(reExportBundle)?.let {
                        bundleFromReExportOrderedListTo(it)
                    }
                }
            }
        }

        requiredBundles.forEach { requiredBundle ->
            result += requiredBundle
            managementService.libReExportRequiredSymbolName[requiredBundle]?.forEach { reExportBundle ->
                result += reExportBundle
                modulesManifest.remove(reExportBundle)?.let {
                    bundleFromReExportOrderedListTo(it)
                }
            }
        }

        return result
    }
