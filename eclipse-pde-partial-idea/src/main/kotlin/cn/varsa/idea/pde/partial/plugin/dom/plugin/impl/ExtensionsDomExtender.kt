package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.dom.plugin.*
import com.intellij.util.xml.reflect.*

class ExtensionsDomExtender : DomExtender<Extension>() {
//    private val logger = thisLogger()

    override fun supportsStubs(): Boolean = false
    override fun registerExtensions(extension: Extension, registrar: DomExtensionsRegistrar) {
//        val file =
//            File("/Users/janewardsandy/eclipse-workspace/测试文件/com.hnintel.casc.rac.exportcard.assemblyDetailsList.xsd")
//
//        val document = SAXBuilder().apply { saxHandlerFactory = NameSpaceCleanerFactory() }.build(file)
//        XPathFactory.instance()
//            .compile("/schema/element[@name='extension']/complexType/sequence/element", Filters.element())
//            .evaluate(document).also { logger.warn("Size: ${it.size}") }.forEach {
//                logger.warn(it.toString())
//
//                registrar.registerCollectionChildrenExtension(
//                    XmlName(it.getAttributeValue("ref")), DomElement::class.java
//                )
//            }
        // TODO: 2021/6/23

        // "fallback" extension
        registrar.registerCustomChildrenExtension(
            Extension.UnresolvedExtension::class.java, CustomDomChildrenDescription.TagNameDescriptor()
        )
    }
}
