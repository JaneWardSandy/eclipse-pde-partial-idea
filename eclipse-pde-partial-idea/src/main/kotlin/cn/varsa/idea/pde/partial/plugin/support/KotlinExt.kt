package cn.varsa.idea.pde.partial.plugin.support

import com.intellij.openapi.module.*
import com.intellij.psi.*

val PsiFile.module: Module? get() = ModuleUtilCore.findModuleForFile(this)
val PsiClass.module: Module? get() = ModuleUtilCore.findModuleForPsiElement(this)
val PsiElement.module: Module? get() = ModuleUtilCore.findModuleForPsiElement(this)
