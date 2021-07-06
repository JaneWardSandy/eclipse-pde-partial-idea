package cn.varsa.idea.pde.partial.plugin.dom.indexes

import cn.varsa.idea.pde.partial.plugin.dom.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.exsd.*
import com.intellij.ide.highlighter.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import java.io.*

class ExtensionPointIndex : SingleEntryFileBasedIndexExtension<ExtensionPointDefinition>() {
    companion object {
        val id =
            ID.create<Int, ExtensionPointDefinition>("cn.varsa.idea.pde.partial.plugin.dom.indexes.ExtensionPointIndex")

        fun readEPDefinition(project: Project, exsdFile: VirtualFile): ExtensionPointDefinition? =
            FileBasedIndex.getInstance().getSingleEntryIndexData(id, exsdFile, project)
    }

    override fun getName(): ID<Int, ExtensionPointDefinition> = id
    override fun getIndexer(): SingleEntryIndexer<ExtensionPointDefinition> = ExtensionPointIndexer
    override fun getValueExternalizer(): DataExternalizer<ExtensionPointDefinition> = ExtensionPointExternalizer
    override fun getVersion(): Int = 0
    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter {
        it.fileType == XmlFileType.INSTANCE && it.extension == "exsd"
    }

    private object ExtensionPointIndexer : SingleEntryIndexer<ExtensionPointDefinition>(false) {
        override fun computeValue(inputData: FileContent): ExtensionPointDefinition? =
            ExtensionPointCacheService.resolveExtensionPoint(inputData.file, inputData.content.inputStream())
    }

    private object ExtensionPointExternalizer : DataExternalizer<ExtensionPointDefinition> {
        override fun save(out: DataOutput, value: ExtensionPointDefinition) = value.save(out)
        override fun read(input: DataInput): ExtensionPointDefinition = ExtensionPointDefinition(input)
    }
}
