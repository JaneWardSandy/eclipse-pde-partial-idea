package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.psi.*

val Project.psiManager: PsiManager get() = PsiManager.getInstance(this)

fun Project.allPDEModules(vararg exclude: Module? = emptyArray()): Set<Module> =
    allModules().filterNot(exclude::contains).filter { it.isLoaded }.filter { PDEFacet.getInstance(it) != null }.toSet()

fun Project.allPDEModulesSymbolicName(vararg exclude: Module? = emptyArray()): Set<String> =
    allPDEModules(*exclude).mapNotNull { BundleManifestCacheService.getInstance(this).getManifest(it) }
        .mapNotNull { it.bundleSymbolicName?.key }.toHashSet()

fun Project.libraryTable(): LibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(this)
