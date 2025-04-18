package cn.varsa.idea.pde.partial.plugin.dom.plugin

import cn.varsa.idea.pde.partial.plugin.dom.plugin.impl.*
import com.intellij.util.xml.*

interface Extension : ExtensionElement {
  companion object {
    const val pointAttribute = "point"
  }

  @Required @Attribute(pointAttribute) @Convert(ExtensionPointConverter::class)
  fun getPoint(): GenericAttributeValue<String>
}
