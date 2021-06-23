package cn.varsa.idea.pde.partial.plugin.dom.plugin

import cn.varsa.idea.pde.partial.plugin.dom.*
import com.intellij.util.xml.*

@DefinesXml
interface OsgiPlugin : DomElementWithTag {
    companion object {
        const val tagName = "plugin"
    }

    @SubTagList("extension-point") fun extensionPoints(): List<ExtensionPoint>
    @SubTagList("extension") fun getExtensions(): List<Extension>
}
