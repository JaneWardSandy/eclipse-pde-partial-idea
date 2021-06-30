package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.openapi.module.*
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class KotlinPackageAccessibilityInspection : PackageAccessibilityInspection() {

    override fun checkElement(
        place: PsiElement, dependency: PsiElement, facet: PDEFacet, occurProblem: (Problem, PsiElement) -> Unit
    ) {
        when (dependency) {
            is KtNamedDeclaration -> {
                checkAccessibility(dependency, facet)?.also { occurProblem(it, place) }

                dependency.getValueParameters().mapNotNull { it.typeReference?.classForRefactor() }
                    .forEach { clazz -> checkAccessibility(clazz, facet)?.also { occurProblem(it, place) } }
                dependency.takeIf { it is KtCallableDeclaration || it is KtClassOrObject }?.getReturnTypeReference()
                    ?.classForRefactor()?.let { checkAccessibility(it, facet) }?.also { occurProblem(it, place) }
            }
        }
    }

    companion object {
        fun checkAccessibility(namedDeclaration: KtNamedDeclaration, facet: PDEFacet): Problem? {
            val targetFile = namedDeclaration.containingFile
            if (targetFile !is KtFile) return null

            val packageName = targetFile.packageFqName.asString()
            if (packageName.isBlank() || packageName.startsWith(Kotlin)) return null

            val qualifiedName = namedDeclaration.fqName?.asString() ?: return null

            if (facet.module == ModuleUtilCore.findModuleForPsiElement(namedDeclaration)) return null

            return checkAccessibility(targetFile, packageName, qualifiedName, facet.module)
        }
    }
}
