package cn.varsa.idea.pde.partial.plugin.dom.plugin

import com.intellij.psi.xml.*
import com.intellij.util.xml.*

interface ExtensionPoint : DomElement {
    companion object {
        const val idAttribute = "id"
        const val nameAttribute = "name"
        const val schemaAttribute = "schema"
    }

    override fun getXmlTag(): XmlTag
    @Required @Attribute(idAttribute) fun getId(): GenericAttributeValue<String>
    @Required @Attribute(nameAttribute) fun getName(): GenericAttributeValue<String>
    @Required @Attribute(schemaAttribute) fun getSchema(): GenericAttributeValue<String>
}
