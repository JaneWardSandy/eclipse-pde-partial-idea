package cn.varsa.idea.pde.partial.inspection.kotlin

import cn.varsa.idea.pde.partial.inspection.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.*

class KotlinPackageAccessibilityInspection : AbstractKotlinInspection(), PackageAccessibilityInspector {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : KtVisitorVoid() {
    override fun visitUserType(type: KtUserType) {
      super.visitUserType(type)
      if (notContainPDEFacet(type)) return

      type.referenceExpression?.also { visitKtElement(it) }
      type.typeArguments.mapNotNull { it.typeReference?.typeElement }.forEach { visitKtElement(it) }
    }

    override fun visitFunctionType(type: KtFunctionType) {
      super.visitFunctionType(type)
      if (notContainPDEFacet(type)) return

      type.contextReceiversTypeReferences.mapNotNull { it.typeElement }.forEach { visitKtElement(it) }
      type.receiverTypeReference?.typeElement?.also { visitKtElement(it) }
      type.parameters.mapNotNull { it.typeReference?.typeElement }.forEach { visitKtElement(it) }
      type.returnTypeReference?.typeElement?.also { visitKtElement(it) }
    }

    override fun visitIntersectionType(intersectionType: KtIntersectionType) {
      super.visitIntersectionType(intersectionType)
      if (notContainPDEFacet(intersectionType)) return

      intersectionType.getLeftTypeRef()?.typeElement?.also { visitKtElement(it) }
      intersectionType.getRightTypeRef()?.typeElement?.also { visitKtElement(it) }
    }

    override fun visitNullableType(nullableType: KtNullableType) {
      super.visitNullableType(nullableType)
      if (notContainPDEFacet(nullableType)) return

      nullableType.innerType?.also { visitKtElement(it) }
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
      super.visitSimpleNameExpression(expression)
      if (notContainPDEFacet(expression)) return

      when (val element = expression.mainReference.resolve()) {
        is PsiClass -> checkManifest(expression, element, holder)

        is PsiMethod -> {
          val returnType = element.returnType
          if (returnType != null && returnType is PsiClassType) {
            returnType.resolve()?.also { checkManifest(expression, it, holder) }
          }

          element.parameterList.parameters.mapNotNull { it.type }.mapNotNull { it as? PsiClassType? }
            .mapNotNull { it.resolve() }.forEach { checkManifest(expression, it, holder) }
        }

        is KtProperty -> {
          val parent = element.parent
          if (parent is KtFile) checkManifest(expression, parent, holder)
        }

        is KtFunction -> {
          val parent = element.parent
          if (parent is KtFile) checkManifest(expression, parent, holder)

          element.receiverTypeReference?.let { visitTypeReference(it) }

          element.valueParameters.mapNotNull { it.typeReference }.forEach { visitTypeReference(it) }
        }
      }
    }
  }
}