package cn.varsa.idea.pde.partial.plugin.dom

import com.intellij.psi.xml.*
import com.intellij.util.xml.*

interface PluginDomElement : DomElement {
  override fun getXmlTag(): XmlTag
}
