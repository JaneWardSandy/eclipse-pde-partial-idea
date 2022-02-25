package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.psi.*

val Project.psiManager: PsiManager get() = PsiManager.getInstance(this)

fun Project.allPDEModules(vararg exclude: Module? = emptyArray()): Set<Module> =
    allModules().filterNot(exclude::contains).filter { it.isLoaded }.filter { PDEFacet.getInstance(it) != null }.toSet()

fun Project.allPDEModulesSymbolicName(
    vararg exclude: Module? = emptyArray(), additionalFilter: (BundleManifest) -> Boolean = { true }
): Set<String> = allPDEModules(*exclude).mapNotNull { BundleManifestCacheService.getInstance(this).getManifest(it) }
    .filter { additionalFilter(it) }.mapNotNull { it.bundleSymbolicName?.key }.toHashSet()

fun Project.libraryTable(): LibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(this)

fun Project.allModules(): List<Module> = ModuleManager.getInstance(this).modules.toList()
fun Module.getModuleDir(): String = guessModuleDir()?.presentableUrl!!

fun Project.fragmentHostManifest(
    fragment: BundleManifest, vararg exclude: Module? = emptyArray()
): BundleManifest? = fragment.fragmentHostAndVersionRange()?.let { (fragmentHostBSN, fragmentHostVersion) ->
    allPDEModules(*exclude).mapNotNull { BundleManifestCacheService.getInstance(this).getManifest(it) }
        .firstOrNull { it.isFragmentHost(fragmentHostBSN, fragmentHostVersion) } ?: BundleManagementService.getInstance(
        this
    ).getBundlesByBSN(fragmentHostBSN, fragmentHostVersion)?.manifest
}
