package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.dom.inspection.*
import cn.varsa.idea.pde.partial.plugin.dom.plugin.*
import com.intellij.icons.*
import com.intellij.util.xml.*
import com.intellij.util.xml.highlighting.*
import javax.swing.*

class PluginDescriptorDomFileDescription : DomFileDescription<OsgiPlugin>(OsgiPlugin::class.java, OsgiPlugin.tagName) {
    override fun getFileIcon(flags: Int): Icon = AllIcons.Nodes.Plugin
    override fun createAnnotator(): DomElementsAnnotator = ElementOccurLimitAnnotator()
}
