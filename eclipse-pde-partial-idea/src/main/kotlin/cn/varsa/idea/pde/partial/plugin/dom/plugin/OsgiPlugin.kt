package cn.varsa.idea.pde.partial.plugin.dom.plugin

import com.intellij.psi.xml.*
import com.intellij.util.xml.*

@DefinesXml
interface OsgiPlugin : DomElement {
    companion object {
        const val tagName = "plugin"
    }

    override fun getXmlTag(): XmlTag

    @SubTagList("extension-point") fun getExtensionPoints(): List<ExtensionPoint>
    fun addExtensionPoint(): ExtensionPoint

    @SubTagList("extension") fun getExtensions(): List<Extension>
    fun addExtension(): Extension
}
