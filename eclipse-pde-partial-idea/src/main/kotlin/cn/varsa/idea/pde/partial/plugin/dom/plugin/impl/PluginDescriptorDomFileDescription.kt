package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.dom.plugin.*
import com.intellij.icons.*
import com.intellij.util.xml.*
import javax.swing.*

class PluginDescriptorDomFileDescription : DomFileDescription<OsgiPlugin>(OsgiPlugin::class.java, OsgiPlugin.tagName) {
    override fun getFileIcon(flags: Int): Icon = AllIcons.Nodes.Plugin
}
