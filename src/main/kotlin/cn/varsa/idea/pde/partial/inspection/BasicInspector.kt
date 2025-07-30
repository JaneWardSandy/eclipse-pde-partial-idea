package cn.varsa.idea.pde.partial.inspection

import cn.varsa.idea.pde.partial.core.facet.*
import com.intellij.facet.*
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.base.util.*

interface BasicInspector {

  fun notContainPDEFacet(element: PsiElement): Boolean {
    val module = element.module ?: return true
    return FacetManager.getInstance(module).getFacetByType(PDEFacet.FACET_TYPE_ID) == null
  }
}