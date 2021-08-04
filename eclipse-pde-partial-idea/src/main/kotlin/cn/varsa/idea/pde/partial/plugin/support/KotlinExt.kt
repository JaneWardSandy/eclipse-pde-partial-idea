package cn.varsa.idea.pde.partial.plugin.support

import com.intellij.openapi.module.*
import com.intellij.psi.*

val PsiFile.module: Module? get() = ModuleUtilCore.findModuleForFile(this)
val PsiClass.module: Module? get() = ModuleUtilCore.findModuleForPsiElement(this)
val PsiElement.module: Module? get() = ModuleUtilCore.findModuleForPsiElement(this)

inline fun <T, R : Any> Iterable<T>.firstNotNullOrNull(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

inline fun <T, R : Any> Array<T>.firstNotNullOrNull(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}
