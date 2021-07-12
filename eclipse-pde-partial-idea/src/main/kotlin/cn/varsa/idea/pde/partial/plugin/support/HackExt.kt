package cn.varsa.idea.pde.partial.plugin.support

import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.idea.util.projectStructure.allModules as allModulesIdea
import org.jetbrains.kotlin.idea.util.projectStructure.getModuleDir as getModuleDirIdea

// HACK for IDEA 2021.1.1 Cannot access class
fun Project.allModules(): List<Module> = allModulesIdea()
fun Module.getModuleDir(): String = getModuleDirIdea()

val PsiFile.module: Module? get() = ModuleUtilCore.findModuleForFile(this)
val PsiClass.module: Module? get() = ModuleUtilCore.findModuleForPsiElement(this)
val PsiElement.module: Module? get() = ModuleUtilCore.findModuleForPsiElement(this)

fun KtTypeReference.classForRefactor(): KtClass? {
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    val type = bindingContext[BindingContext.TYPE, this] ?: return null
    val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor) as? KtClass
}
