package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.dom.plugin.*

abstract class ExtensionImpl : Extension {
    override fun getExtensionPoint(): ExtensionPoint? = childDescription.domDeclaration?.let { it as? ExtensionPoint }
}
