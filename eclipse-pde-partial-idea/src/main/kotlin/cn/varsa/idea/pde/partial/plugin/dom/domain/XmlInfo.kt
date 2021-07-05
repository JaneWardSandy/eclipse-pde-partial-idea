package cn.varsa.idea.pde.partial.plugin.dom.domain

import com.intellij.openapi.vfs.*

data class XmlInfo(
    val applications: Set<String>,
    val products: Set<String>,
    val epPoint2ExsdPath: Map<String, VirtualFile>,
    val epReferenceIdentityMap: Map<Pair<String, String>, Map<String, Set<String>>>
)
