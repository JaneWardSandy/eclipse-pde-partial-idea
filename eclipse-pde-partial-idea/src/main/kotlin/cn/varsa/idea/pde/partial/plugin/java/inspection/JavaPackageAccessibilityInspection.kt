package cn.varsa.idea.pde.partial.plugin.java.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.inspection.*
import com.intellij.codeInsight.daemon.impl.analysis.*
import com.intellij.openapi.module.*
import com.intellij.psi.*
import java.lang.annotation.*

class JavaPackageAccessibilityInspection : PackageAccessibilityInspection() {

    override fun checkElement(
        place: PsiElement, dependency: PsiElement, facet: PDEFacet, occurProblem: (Problem, PsiElement) -> Unit
    ) {
        when (dependency) {
            is PsiClass -> checkAccessibility(dependency, facet)?.also { occurProblem(it, place) }
            is PsiMethod -> dependency.parent?.let { it as? PsiClass }?.let { checkAccessibility(it, facet) }
                ?.also { occurProblem(it, place) }
        }
    }

    companion object {
        fun checkAccessibility(targetClass: PsiClass, facet: PDEFacet): Problem? {
            var clzz = targetClass
            while (clzz.parent is PsiClass) clzz = clzz.parent as PsiClass

            if (clzz.isAnnotationType) {
                val policy = AnnotationsHighlightUtil.getRetentionPolicy(clzz)
                if (policy == RetentionPolicy.CLASS || policy == RetentionPolicy.SOURCE) return null
            }

            val targetFile = clzz.containingFile
            if (targetFile !is PsiClassOwner) return null

            val packageName = targetFile.packageName
            if (packageName.isBlank() || packageName.startsWith(Java)) return null

            val qualifiedName = clzz.qualifiedName ?: return null

            if (facet.module == ModuleUtilCore.findModuleForPsiElement(clzz)) return null

            return checkAccessibility(targetFile, packageName, qualifiedName, facet.module)
        }
    }
}
