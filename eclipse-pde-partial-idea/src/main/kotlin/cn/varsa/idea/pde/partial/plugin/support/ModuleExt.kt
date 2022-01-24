package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.vfs.*
import com.intellij.util.*
import org.osgi.framework.*

val Module.moduleRootManager: ModuleRootManager get() = ModuleRootManager.getInstance(this)
fun Module.updateModel(task: Consumer<in ModifiableRootModel>) = ModuleRootModificationUtil.updateModel(this, task)
fun VirtualFile.findModule(project: Project) = ModuleUtilCore.findModuleForFile(this, project)

fun Module.findLibrary(predicate: (Library) -> Boolean): Library? =
    ModuleRootManager.getInstance(this).orderEntries.mapNotNull { it as? LibraryOrderEntry }.mapNotNull { it.library }
        .firstOrNull(predicate)

fun Module.isBundleRequiredOrFromReExport(symbolName: String, version: Set<Version> = emptySet()): Boolean {
    val cacheService = BundleManifestCacheService.getInstance(project)
    val managementService = BundleManagementService.getInstance(project)

    val manifest = cacheService.getManifest(this) ?: return false

    // Bundle required directly
    manifest.isBundleRequired(symbolName, version).ifTrue { return true }

    val requiredBundle = manifest.requireBundle?.keys ?: return false
    val allRequiredFromReExport = managementService.getLibReExportRequired(manifest.requiredBundleAndVersion())

    // Re-export dependency tree can resolve bundle
    allRequiredFromReExport.contains(symbolName).ifTrue { return true }

    // Re-export bundle contain module, it needs calc again
    val modulesManifest =
        project.allPDEModules().filterNot { it == this }.mapNotNull(cacheService::getManifest).toHashSet()

    modulesManifest.filter {
        it.bundleSymbolicName?.key?.run { requiredBundle.contains(this) || allRequiredFromReExport.contains(this) } == true
    }.any { isBundleFromReExportOnly(it, symbolName, version, cacheService, managementService, modulesManifest) }
        .ifTrue { return true }

    return false
}

private fun isBundleFromReExportOnly(
    manifest: BundleManifest,
    symbolName: String,
    version: Set<Version>,
    cacheService: BundleManifestCacheService,
    managementService: BundleManagementService,
    modulesManifest: HashSet<BundleManifest>
): Boolean {
    // Re-export directly
    manifest.reexportRequiredBundleAndVersion()
        .filterValues { range -> version.isEmpty() || version.any { range.includes(it) } }.containsKey(symbolName)
        .ifTrue { return true }

    val allReExport = managementService.getLibReExportRequired(manifest.reexportRequiredBundleAndVersion())

    // Re-export dependency tree can resolve bundle
    allReExport.contains(symbolName).ifTrue { return true }

    // Dependency tree contains module, it needs calc again, and remove it from module set to not calc again and again and again
    return modulesManifest.filter { allReExport.contains(it.bundleSymbolicName?.key) }.toSet().also { modulesManifest -= it }
        .any { isBundleFromReExportOnly(it, symbolName, version, cacheService, managementService, modulesManifest) }
}

val Module.bundleRequiredOrFromReExportOrderedList: LinkedHashSet<Pair<String, Version>>
    get() {
        val cacheService = BundleManifestCacheService.getInstance(project)
        val managementService = BundleManagementService.getInstance(project)

        val result = linkedSetOf<Pair<String, Version>>()

        val manifest = cacheService.getManifest(this) ?: return result

        val modulesManifest =
            project.allPDEModules().filterNot { it == this }.mapNotNull { cacheService.getManifest(it) }
                .associate { it.bundleSymbolicName?.key to (it.bundleVersion to it) }.toMutableMap()

        fun processBSN(
            exportBundle: String, range: VersionRange, onEach: (Map.Entry<String, VersionRange>) -> Unit
        ) {
            managementService.getBundlesByBSN(exportBundle, range)
                ?.let { result += it.bundleSymbolicName to it.bundleVersion }

            modulesManifest[exportBundle]?.takeIf { range.includes(it.first) }
                ?.also { modulesManifest -= exportBundle }?.second?.also { result += exportBundle to it.bundleVersion }
                ?.requiredBundleAndVersion()?.forEach { onEach(it) }
        }

        fun cycleBSN(exportBundle: String, range: VersionRange) {
            processBSN(exportBundle, range) { cycleBSN(it.key, it.value) }

            managementService.getLibReExportRequired(exportBundle, range)?.forEach { (bsn, reqRange) ->
                processBSN(bsn, reqRange) { cycleBSN(it.key, it.value) }
            }
        }

        manifest.requiredBundleAndVersion().forEach { cycleBSN(it.key, it.value) }

        return result
    }
