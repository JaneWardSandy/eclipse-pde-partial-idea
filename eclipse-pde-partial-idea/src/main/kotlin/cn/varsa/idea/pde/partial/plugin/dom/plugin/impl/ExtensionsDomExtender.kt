package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.dom.*
import cn.varsa.idea.pde.partial.plugin.dom.config.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.dom.exsd.*
import cn.varsa.idea.pde.partial.plugin.dom.plugin.*
import cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.annotation.*
import cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.annotation.ExtendClass
import cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.annotation.Required
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.extensions.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.*
import com.intellij.psi.search.*
import com.intellij.util.xml.*
import com.intellij.util.xml.converters.*
import com.intellij.util.xml.highlighting.*
import com.intellij.util.xml.reflect.*
import org.jetbrains.annotations.*
import java.io.*
import java.lang.reflect.*

class ExtensionsDomExtender : DomExtender<Extension>() {

  override fun supportsStubs(): Boolean = false
  override fun registerExtensions(extension: Extension, registrar: DomExtensionsRegistrar) {
    val project = extension.module?.project ?: return
    val point = extension.getPoint().stringValue?.takeIf(String::isNotBlank) ?: return

    if (DumbService.isDumb(project)) return

    val managementService = ExtensionPointManagementService.getInstance(extension.xmlTag.project)
    managementService.getExtensionPoint(point)?.also {
      registerElement(it, it.extension, project, registrar, managementService)
    }?.extension?.also { definition ->
      definition.elementRefs.map { ExtensionElement.OccursLimit(it.ref, it.minOccurs, it.maxOccurs) }
        .also { extension.putUserData(ExtensionElement.occursLimitKey, it) }
    }
  }

  private fun registerElement(
    extensionPoint: ExtensionPointDefinition,
    element: ElementDefinition?,
    project: Project,
    registrar: DomExtensionsRegistrar,
    managementService: ExtensionPointManagementService
  ) {
    element?.elementRefs?.mapNotNull { extensionPoint.findRefElement(it, project) }?.forEach { definition ->
      if (definition.attributes.isEmpty() && definition.type == "string") {
        registrar.registerCollectionChildrenExtension(XmlName(definition.name), SimpleTagValue::class.java)
      } else {
        registrar.registerCollectionChildrenExtension(XmlName(definition.name), ExtensionElement::class.java)
          .addExtender(object : DomExtender<ExtensionElement>() {
            override fun registerExtensions(
              t: ExtensionElement, registrar: DomExtensionsRegistrar
            ) {
              registerElement(extensionPoint, definition, project, registrar, managementService)

              definition.elementRefs.map {
                ExtensionElement.OccursLimit(it.ref, it.minOccurs, it.maxOccurs)
              }.also { t.putUserData(ExtensionElement.occursLimitKey, it) }
            }
          })
      }
    }

    element?.attributes?.forEach { definition ->
      val type: Type = when {
        definition.type == "boolean" -> Boolean::class.java
        definition.kind == "java" -> PsiClass::class.java
        else -> String::class.java
      }
      val childExtension = registrar.registerGenericAttributeValueChildExtension(XmlName(definition.name), type)

      if (definition.use == "required") childExtension.addCustomAnnotation(Required.INSTANCE)

      if (definition.kind == "identifier" || definition.simpleBaseType == "string") {
        val references =
          (definition.simpleEnumeration ?: emptyList()) + (definition.basedOn?.split(',')?.map { it.split('/') }
            ?.filter { it.size == 3 }
            ?.flatMap { managementService.getReferenceIdentifies(it[0], it[1], it[2].substringAfter('@')) }?.sorted()
            ?: emptyList())
        childExtension.addCustomAnnotation(NoSpellchecking.INSTANCE)
        childExtension.setConverter(StringListConverter(references))
      } else if (definition.kind == "java") {
        definition.basedOn?.split(':')?.filter(String::isNotBlank)?.takeIf(List<String>::isNotEmpty)?.toTypedArray()
          ?.let(::ExtendClass)?.also(childExtension::addCustomAnnotation)
        childExtension.setConverter(ClassConverter)
      } else if (definition.kind == "resource") {
        childExtension.addCustomAnnotation(NoSpellchecking.INSTANCE)
        childExtension.setConverter(PathReferenceConverter.INSTANCE)
      }
    }
  }

  interface SimpleTagValue : GenericDomValue<String>

  class StringListConverter(private val values: Collection<String>) : ResolvingConverter.StringConverter() {
    override fun getVariants(context: ConvertContext): MutableCollection<out String> = values.toMutableList()
  }

  object ClassConverter : PsiClassConverter() {
    override fun getScope(context: ConvertContext): GlobalSearchScope = GlobalSearchScope.allScope(context.project)

    override fun createClassReferenceProvider(
      genericDomValue: GenericDomValue<PsiClass>?,
      context: ConvertContext,
      extendClass: com.intellij.util.xml.ExtendClass?
    ): JavaClassReferenceProvider {
      val provider = object : JavaClassReferenceProvider() {
        override fun getScope(project: Project): GlobalSearchScope = getScope(context)
        override fun getReferencesByString(
          str: String?, position: PsiElement, offsetInPosition: Int
        ): Array<PsiReference> = super.getReferencesByString(str?.substringBefore(':'), position, offsetInPosition)
      }
      provider.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, java.lang.Boolean.TRUE)

      return createJavaClassReferenceProvider(genericDomValue, extendClass, provider)
    }
  }
}
