package cn.varsa.idea.pde.partial.plugin.dom.domain

import com.intellij.openapi.vfs.*

data class XmlInfo(
    val applications: HashSet<String>,
    val products: HashSet<String>,
    val epPoint2ExsdPath: HashMap<String, VirtualFile>,
    val epReferenceIdentityMap: HashMap<Pair<String, String>, HashMap<String, HashSet<String>>>
)
