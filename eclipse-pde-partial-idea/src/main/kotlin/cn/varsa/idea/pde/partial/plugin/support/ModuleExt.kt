package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.module.*
import org.jetbrains.kotlin.idea.util.*

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
        val requiredBundle = manifest.requireBundle?.keys ?: return result

        // Bundle required directly
        manifest.requireBundle?.keys?.also { result += it }

        result += requiredBundle.mapNotNull { managementService.libReExportRequiredSymbolName[it] }.flatten().toSet()

        // Re-export bundle contain module, it need calc again
        val modulesManifest =
            project.allPDEModules().filterNot { it == this }.mapNotNull(cacheService::getManifest).toHashSet()

        modulesManifest.filter {
            it.bundleSymbolicName?.key?.run { requiredBundle.contains(this) || result.contains(this) } == true
        }.forEach { bundleFromReExportOrderedListTo(it, cacheService, managementService, modulesManifest, result) }

        return result
    }

private fun bundleFromReExportOrderedListTo(
    manifest: BundleManifest,
    cacheService: BundleManifestCacheService,
    managementService: BundleManagementService,
    modulesManifest: HashSet<BundleManifest>,
    result: LinkedHashSet<String>
) {
    // Re-export directly
    result += manifest.reExportRequiredBundleSymbolNames

    result += manifest.reExportRequiredBundleSymbolNames.mapNotNull { managementService.libReExportRequiredSymbolName[it] }
        .flatten().toSet()

    // Dependency tree contains module, it need calc again, and remove it from module set to not calc again and again and again
    modulesManifest.filter { result.contains(it.bundleSymbolicName?.key) }.also { modulesManifest -= it }
        .forEach { bundleFromReExportOrderedListTo(it, cacheService, managementService, modulesManifest, result) }
}
