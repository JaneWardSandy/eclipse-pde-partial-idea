package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.ManifestPath
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.PDEFacet
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

fun Module.getManifestFile(): VirtualFile? {
  val relativePath = PDEFacet.getInstance(this)?.configuration?.manifestRelativePath ?: ManifestPath
  return moduleRootManager.contentRoots.firstNotNullOfOrNull { it.findFileByRelativePath(relativePath) }
}

fun Module.findLibrary(predicate: (Library) -> Boolean): Library? =
  ModuleRootManager.getInstance(this).orderEntries.mapNotNull { it as? LibraryOrderEntry }.mapNotNull { it.library }
    .firstOrNull(predicate)

fun Module.isBundleRequiredOrFromReExport(symbolName: String, version: Set<Version> = emptySet()): Boolean =
  BundleManifestCacheService.getInstance(project).getManifest(this)
    ?.isBundleRequiredOrFromReExport(project, this, symbolName, version) ?: false

val Module.bundleRequiredOrFromReExportOrderedList: LinkedHashSet<Pair<String, Version>>
  get() {
    val cacheService = BundleManifestCacheService.getInstance(project)
    val managementService = BundleManagementService.getInstance(project)

    val result = linkedSetOf<Pair<String, Version>>()

    val manifest = cacheService.getManifest(this) ?: return result

    val modulesManifest = project.allPDEModules(this).mapNotNull { cacheService.getManifest(it) }
      .associate { it.bundleSymbolicName?.key to (it.bundleVersion to it) }.toMutableMap()

    fun processBSN(
      exportBundle: String, range: VersionRange, onEach: (Map.Entry<String, VersionRange>) -> Unit
    ) {
      managementService.getBundlesByBSN(exportBundle, range)
        ?.let { result += it.bundleSymbolicName to it.bundleVersion }

      modulesManifest[exportBundle]?.takeIf { it.first in range }
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
