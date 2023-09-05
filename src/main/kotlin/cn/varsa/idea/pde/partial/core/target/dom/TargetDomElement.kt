package cn.varsa.idea.pde.partial.core.target.dom

import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomElement

interface TargetDomElement : DomElement {
  override fun getXmlTag(): XmlTag
}
