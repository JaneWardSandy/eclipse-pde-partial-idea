package cn.varsa.idea.pde.partial.plugin.kotlin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.inspection.*
import cn.varsa.idea.pde.partial.plugin.kotlin.support.*
import com.intellij.openapi.module.*
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class KotlinPackageAccessibilityInspection : PackageAccessibilityInspection() {

    override fun checkElement(
        place: PsiElement, dependency: PsiElement, facet: PDEFacet, occurProblem: (Problem, PsiElement) -> Unit
    ) {
        when (dependency) {
            is KtNamedDeclaration -> {
                checkAccessibility(dependency, facet).forEach { occurProblem(it, place) }

                dependency.getValueParameters().mapNotNull { it.typeReference?.classForRefactor() }
                    .forEach { clazz -> checkAccessibility(clazz, facet).forEach { occurProblem(it, place) } }
                dependency.takeIf { it is KtCallableDeclaration || it is KtClassOrObject }?.getReturnTypeReference()
                    ?.classForRefactor()?.let { checkAccessibility(it, facet) }?.forEach { occurProblem(it, place) }
            }
        }
    }

    companion object {
        fun checkAccessibility(namedDeclaration: KtNamedDeclaration, facet: PDEFacet): List<Problem> {
            val targetFile = namedDeclaration.containingKtFile

            val packageName = targetFile.packageFqName.asString()
            if (packageName.isBlank() || packageName.startsWith(Kotlin)) return emptyList()

            val qualifiedName = namedDeclaration.fqName?.asString() ?: return emptyList()

            if (facet.module == ModuleUtilCore.findModuleForPsiElement(namedDeclaration)) return emptyList()

            return checkAccessibility(targetFile, packageName, qualifiedName, facet.module)
        }
    }
}
