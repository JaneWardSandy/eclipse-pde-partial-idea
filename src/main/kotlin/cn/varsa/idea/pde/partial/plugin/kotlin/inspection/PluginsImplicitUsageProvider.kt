package cn.varsa.idea.pde.partial.plugin.kotlin.inspection

import cn.varsa.idea.pde.partial.plugin.dom.config.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInsight.daemon.*
import com.intellij.psi.*

class PluginsImplicitUsageProvider : ImplicitUsageProvider {
  override fun isImplicitRead(element: PsiElement): Boolean = false
  override fun isImplicitWrite(element: PsiElement): Boolean = false

  override fun isImplicitUsage(element: PsiElement): Boolean = (element as? PsiClass)?.let { clazz ->
    clazz.module?.project?.let { ExtensionPointManagementService.getInstance(it) }
      ?.isUsageByAnyExtension(clazz.qualifiedName)
  } ?: false
}
