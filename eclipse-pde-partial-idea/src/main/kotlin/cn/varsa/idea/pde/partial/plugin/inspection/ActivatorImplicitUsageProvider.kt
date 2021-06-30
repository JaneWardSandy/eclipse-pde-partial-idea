package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInsight.daemon.*
import com.intellij.psi.*

class ActivatorImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false

    override fun isImplicitUsage(element: PsiElement): Boolean = (element as? PsiClass)?.let { clazz ->
        clazz.module?.let {
            BundleManifestCacheService.getInstance(it.project).getManifest(it)
        }?.bundleActivator == clazz.qualifiedName
    } ?: false
}
