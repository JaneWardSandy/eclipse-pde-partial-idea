package cn.varsa.idea.pde.partial.inspection

import cn.varsa.idea.pde.partial.core.facet.PDEFacet
import com.intellij.facet.FacetManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.util.module

interface BasicInspector {

  fun notContainPDEFacet(element: PsiElement): Boolean {
    val module = element.module ?: return true
    return FacetManager.getInstance(module).getFacetByType(PDEFacet.FACET_TYPE_ID) == null
  }
}