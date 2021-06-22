package cn.varsa.idea.pde.partial.plugin.dom.plugin

import com.intellij.psi.xml.*
import com.intellij.util.xml.*

interface Extension : DomElement {
    companion object {
        const val pointAttribute = "point"
    }

    override fun getXmlTag(): XmlTag
    @Required @Attribute(pointAttribute) fun getPoint(): GenericAttributeValue<String>
    fun getExtensionPoint(): ExtensionPoint?
}

/**
 * Special marker for extension that cannot be resolved using current dependencies.
 */
interface UnresolvedExtension : DomElement
