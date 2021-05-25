package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*

fun Project.allPDEModules(): Set<Module> =
    allModules().filter { it.isLoaded }.filter { PDEFacet.getInstance(it) != null }.toSet()
