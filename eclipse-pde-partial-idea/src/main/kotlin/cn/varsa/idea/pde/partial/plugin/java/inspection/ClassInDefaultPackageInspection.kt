package cn.varsa.idea.pde.partial.plugin.java.inspection

import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.inspection.*
import com.intellij.codeInspection.*
import com.intellij.psi.*

class ClassInDefaultPackageInspection : AbstractOsgiVisitor() {
  override fun buildVisitor(facet: PDEFacet, holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    object : PsiElementVisitor() {
      override fun visitFile(file: PsiFile) {
        (file as? PsiClassOwner)?.takeIf { it.packageName.isBlank() }?.classes?.takeIf { it.isNotEmpty() }
          ?.firstOrNull()?.also {
            val identifier = unwrap(it.nameIdentifier)
            if (identifier != null && isValidElement(identifier)) {
              holder.registerProblem(identifier, message("inspection.hint.classInDefaultPackage"))
            }
          }
      }
    }
}
