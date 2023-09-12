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

        val element = reference.resolve()
        if (element is PsiClass) {
          checkManifest(reference, element, holder)
        }
      }

      override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        super.visitMethodCallExpression(expression)

        val returnType = expression.type
        if (returnType != null && returnType is PsiClassType) {
          val clazz = returnType.resolve()
          if (clazz != null) {
            checkManifest(expression, clazz, holder)
          }
        }

        expression.argumentList.expressionTypes
          .mapNotNull { it as? PsiClassType? }
          .mapNotNull { it.resolve() }
          .forEach { checkManifest(expression, it, holder) }
      }

      override fun visitLambdaExpression(expression: PsiLambdaExpression) {
        super.visitLambdaExpression(expression)

        expression.parameterList.parameters
          .mapNotNull { it.type as? PsiClassType? }
          .mapNotNull { it.resolve() }
          .forEach { checkManifest(expression, it, holder) }

        val interfaceType = expression.functionalInterfaceType
        if (interfaceType != null && interfaceType is PsiClassType) {
          val clazz = interfaceType.resolve()
          if (clazz != null) {
            checkManifest(expression, clazz, holder)
          }
        }
      }
    }
}