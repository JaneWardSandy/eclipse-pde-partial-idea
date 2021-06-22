package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.dom.plugin.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.xml.*
import com.intellij.util.xml.reflect.*
import com.intellij.xml.impl.schema.*
import java.io.*

class ExtensionsDomExtender : DomExtender<Extension>() {
    override fun supportsStubs(): Boolean = false
    override fun registerExtensions(extension: Extension, registrar: DomExtensionsRegistrar) {
//        val file =
//            File("/Users/janewardsandy/eclipse-workspace/测试文件/com.hnintel.casc.rac.exportcard.assemblyDetailsList.xsd")
//        val schema = VfsUtil.findFileByIoFile(file, true)
//        if (schema == null) {
//            thisLogger().warn("fileNotExisted: $file")
//            return
//        }
//        val psiFile = PsiManager.getInstance(extension.manager.project).findFile(schema)
//        if (psiFile !is XmlFile) {
//            thisLogger().warn("File Not Xml File: $file")
//            return
//        }
//        val nsDescriptor = SchemaNSDescriptor().apply { init(psiFile) }
//        val descriptor = nsDescriptor.getElementDescriptor("extension", "com.hnintel.casc.rac")
//        thisLogger().warn("$descriptor")

        // TODO: 2021/6/22

        // "fallback" extension
        registrar.registerCustomChildrenExtension(
            UnresolvedExtension::class.java, CustomDomChildrenDescription.TagNameDescriptor()
        )
    }
}
