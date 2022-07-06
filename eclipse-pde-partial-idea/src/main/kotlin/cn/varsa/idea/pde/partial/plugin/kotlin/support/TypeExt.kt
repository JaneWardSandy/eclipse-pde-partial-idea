package cn.varsa.idea.pde.partial.plugin.kotlin.support

import cn.varsa.idea.pde.partial.plugin.support.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.codeInsight.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.*

fun KtTypeReference.classForRefactor(): KtClass? {
  val bindingContext = analyze(BodyResolveMode.PARTIAL)
  val type = bindingContext[BindingContext.TYPE, this] ?: return null
  val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
  return module?.project?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(it, classDescriptor) } as? KtClass
}
