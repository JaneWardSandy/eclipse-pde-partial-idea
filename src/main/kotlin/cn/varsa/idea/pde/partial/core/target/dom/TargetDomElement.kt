package cn.varsa.idea.pde.partial.core.target.dom

import com.intellij.psi.xml.*
import com.intellij.util.xml.*

interface TargetDomElement : DomElement {
  override fun getXmlTag(): XmlTag
}
