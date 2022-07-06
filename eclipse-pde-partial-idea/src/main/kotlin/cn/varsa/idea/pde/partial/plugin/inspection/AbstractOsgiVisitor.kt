package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInspection.*
import com.intellij.psi.*

abstract class AbstractOsgiVisitor : LocalInspectionTool() {
  abstract fun buildVisitor(facet: PDEFacet, holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    holder.file.module?.let { PDEFacet.getInstance(it) }?.let { buildVisitor(it, holder, isOnTheFly) }
      ?: PsiElementVisitor.EMPTY_VISITOR

  internal fun unwrap(element: PsiElement?): PsiElement? =
    element?.takeUnless { element.isPhysical }?.navigationElement ?: element

  internal fun isValidElement(element: PsiElement?): Boolean =
    element != null && element.isPhysical && element.text.isNotBlank()
}
