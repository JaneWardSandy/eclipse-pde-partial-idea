package cn.varsa.idea.pde.partial.plugin.dom.plugin

import cn.varsa.idea.pde.partial.plugin.dom.*
import com.intellij.util.xml.*

interface ExtensionPoint : PluginDomElement {
    companion object {
        const val idAttribute = "id"
        const val nameAttribute = "name"
        const val schemaAttribute = "schema"
    }

    @Required @Attribute(idAttribute) fun getId(): GenericAttributeValue<String>
    @Required @Attribute(nameAttribute) fun getName(): GenericAttributeValue<String>
    @Required @Attribute(schemaAttribute) fun getSchema(): GenericAttributeValue<String>
}
