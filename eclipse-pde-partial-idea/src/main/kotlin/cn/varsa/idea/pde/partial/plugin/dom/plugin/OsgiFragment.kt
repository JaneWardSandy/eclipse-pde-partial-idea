package cn.varsa.idea.pde.partial.plugin.dom.plugin

import cn.varsa.idea.pde.partial.plugin.dom.*
import com.intellij.util.xml.*

@DefinesXml
interface OsgiFragment : PluginDomElement {
  companion object {
    const val tagName = "fragment"
  }

  @SubTagList("extension-point") fun getExtensionPoints(): List<ExtensionPoint>
  @SubTagList("extension") fun getExtensions(): List<Extension>
}
