package cn.varsa.idea.pde.partial.plugin.dom.plugin

import cn.varsa.idea.pde.partial.plugin.dom.*
import com.intellij.util.xml.*

interface Extension : DomElementWithTag {
    companion object {
        const val pointAttribute = "point"
    }

    @Required @Attribute(pointAttribute) fun getPoint(): GenericAttributeValue<String>
    fun getExtensionPoint(): ExtensionPoint?

    /**
     * Special marker for extension that cannot be resolved using current dependencies.
     */
    interface UnresolvedExtension : DomElement
}
