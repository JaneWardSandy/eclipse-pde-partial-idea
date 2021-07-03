package cn.varsa.idea.pde.partial.plugin.dom.plugin

import cn.varsa.idea.pde.partial.plugin.dom.*
import com.intellij.openapi.util.*

interface ExtensionElement : PluginDomElement {
    companion object {
        val occursLimitKey = Key.create<List<OccursLimit>>("occursLimitsKey")
    }

    data class OccursLimit(val tagName: String, val minOccurs: Int = 1, val maxOccurs: Int = 1) {
        companion object {
            const val unbounded = -1
        }
    }
}
