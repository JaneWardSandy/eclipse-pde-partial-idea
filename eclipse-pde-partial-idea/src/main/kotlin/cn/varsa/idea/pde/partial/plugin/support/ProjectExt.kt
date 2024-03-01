package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.facet.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.libraries.*
import org.jetbrains.kotlin.idea.util.projectStructure.*

fun Project.allPDEModules(vararg exclude: Module? = emptyArray()): Set<Module> =
  allModules().filterNot(exclude::contains).filter { it.isLoaded }
    .filter { module -> FacetManager.getInstance(module).allFacets.any { it.typeId == PDEFacetType.id } }.toSet()

fun Project.allPDEModulesSymbolicName(
  vararg exclude: Module? = emptyArray(), additionalFilter: (BundleManifest) -> Boolean = { true }
): Set<String> = allPDEModules(*exclude).mapNotNull { BundleManifestCacheService.getInstance(this).getManifest(it) }
  .filter { additionalFilter(it) }.mapNotNull { it.bundleSymbolicName?.key }.toHashSet()

fun Project.libraryTable(): LibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(this)

fun Module.getModuleDir(): String? = guessModuleDir()?.presentableUrl

fun Project.fragmentHostManifest(
  fragment: BundleManifest, vararg exclude: Module? = emptyArray()
): BundleManifest? = fragment.fragmentHostAndVersionRange()?.let { (fragmentHostBSN, fragmentHostVersion) ->
  allPDEModules(*exclude).mapNotNull { BundleManifestCacheService.getInstance(this).getManifest(it) }
    .firstOrNull { it.isFragmentHost(fragmentHostBSN, fragmentHostVersion) } ?: BundleManagementService.getInstance(
    this
  ).getBundlesByBSN(fragmentHostBSN, fragmentHostVersion)?.manifest
}
