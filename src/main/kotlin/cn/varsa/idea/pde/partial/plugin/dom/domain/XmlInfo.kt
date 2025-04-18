package cn.varsa.idea.pde.partial.plugin.dom.domain

import com.intellij.openapi.vfs.*
import com.jetbrains.rd.util.*

data class XmlInfo(
  val applications: HashSet<String>,
  val products: HashSet<String>,
  val epPoint2ExsdPath: HashMap<String, VirtualFile>,
  val epReferenceIdentityMap: ConcurrentHashMap<Pair<String, String>, ConcurrentHashMap<String, HashSet<String>>>
)
