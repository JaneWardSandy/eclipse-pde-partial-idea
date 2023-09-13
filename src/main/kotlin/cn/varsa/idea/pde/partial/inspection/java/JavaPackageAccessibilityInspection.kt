package cn.varsa.idea.pde.partial.inspection.java

import cn.varsa.idea.pde.partial.inspection.PackageAccessibilityInspector
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

class JavaPackageAccessibilityInspection : AbstractBaseJavaLocalInspectionTool(), PackageAccessibilityInspector {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    object : JavaElementVisitor() {
      override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        super.visitReferenceElement(reference)
        if (notContainPDEFacet(reference)) return

        val element = reference.resolve()
        if (element is PsiClass) checkManifest(reference, element, holder)
      }

      override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        super.visitMethodCallExpression(expression)
        if (notContainPDEFacet(expression)) return

        expression.resolveMethod()?.containingClass?.also { checkManifest(expression, it, holder) }

        val returnType = expression.type
        if (returnType != null && returnType is PsiClassType) {
          returnType.resolve()?.also { checkManifest(expression, it, holder) }
        }

        expression.argumentList.expressionTypes
          .mapNotNull { it as? PsiClassType? }
          .mapNotNull { it.resolve() }
          .forEach { checkManifest(expression, it, holder) }
      }

      override fun visitLambdaExpression(expression: PsiLambdaExpression) {
        super.visitLambdaExpression(expression)
        if (notContainPDEFacet(expression)) return

        expression.parameterList.parameters
          .mapNotNull { it.type as? PsiClassType? }
          .mapNotNull { it.resolve() }
          .forEach { checkManifest(expression, it, holder) }

        val interfaceType = expression.functionalInterfaceType
        if (interfaceType != null && interfaceType is PsiClassType) {
          interfaceType.resolve()?.also { checkManifest(expression, it, holder) }
        }
      }
    }
}