package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.psi.*

val Project.psiManager: PsiManager get() = PsiManager.getInstance(this)

fun Project.allPDEModules(): Set<Module> =
    allModules().filter { it.isLoaded }.filter { PDEFacet.getInstance(it) != null }.toSet()

fun Project.allPDEModulesSymbolicName(): Set<String> =
    allPDEModules().mapNotNull { BundleManifestCacheService.getInstance(this).getManifest(it) }
        .mapNotNull { it.bundleSymbolicName?.key }.toHashSet()

fun Project.libraryTable(): LibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(this)
