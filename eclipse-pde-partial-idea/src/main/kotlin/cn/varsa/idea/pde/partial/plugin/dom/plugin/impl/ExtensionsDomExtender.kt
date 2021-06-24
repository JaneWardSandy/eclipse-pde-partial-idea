package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.dom.exsd.*
import cn.varsa.idea.pde.partial.plugin.dom.plugin.*
import com.intellij.openapi.vfs.*
import com.intellij.util.xml.*
import com.intellij.util.xml.reflect.*
import java.io.*

class ExtensionsDomExtender : DomExtender<Extension>() {

    override fun supportsStubs(): Boolean = false
    override fun registerExtensions(extension: Extension, registrar: DomExtensionsRegistrar) {
        val file = File("/Users/janewardsandy/eclipse-workspace/测试文件/commands.exsd")
        VfsUtil.findFileByIoFile(file, false)?.let { ExtensionPointDefinition(it) }?.also {
            registerElement(it, it.extension, registrar)
        }

        // TODO: 2021/6/24
    }

    private fun registerElement(
        extension: ExtensionPointDefinition, element: ElementDefinition, registrar: DomExtensionsRegistrar
    ) {
        element.elementRefs.mapNotNull { ref -> extension.elements.firstOrNull { it.name == ref.ref } }.forEach {
            registrar.registerCollectionChildrenExtension(XmlName(it.name), DomElement::class.java)
                .addExtender(object : DomExtender<DomElement>() {
                    override fun registerExtensions(t: DomElement, registrar: DomExtensionsRegistrar) {
                        registerElement(extension, it, registrar)
                    }
                })
        }

        element.attributes.forEach {
            registrar.registerGenericAttributeValueChildExtension(XmlName(it.name), String::class.java)
        }
    }
}
