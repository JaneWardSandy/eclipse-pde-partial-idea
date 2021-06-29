package cn.varsa.idea.pde.partial.plugin.dom.plugin

import cn.varsa.idea.pde.partial.plugin.dom.*
import cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.*
import com.intellij.util.xml.*

interface Extension : DomElementWithTag {
    companion object {
        const val pointAttribute = "point"
    }

    @Required @Attribute(pointAttribute) @Convert(ExtensionPointConverter::class)
    fun getPoint(): GenericAttributeValue<String>

    /**
     * Special marker for extension that cannot be resolved using current dependencies.
     */
    interface UnresolvedExtension : DomElement
}
