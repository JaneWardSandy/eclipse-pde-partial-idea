package cn.varsa.idea.pde.partial.plugin.dom.indexes

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.dom.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.ide.highlighter.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import com.jetbrains.rd.util.*
import java.io.*

class PluginXmlIndex : SingleEntryFileBasedIndexExtension<XmlInfo>() {
    companion object {
        val id = ID.create<Int, XmlInfo>("cn.varsa.idea.pde.partial.plugin.dom.indexes.PluginXmlIndex")

        fun readXmlInfo(project: Project, pluginXml: VirtualFile): XmlInfo? =
            FileBasedIndex.getInstance().getFileData(id, pluginXml, project).firstOrNull()?.value
    }

    override fun getName(): ID<Int, XmlInfo> = id
    override fun getIndexer(): SingleEntryIndexer<XmlInfo> = PluginXmlIndexer
    override fun getValueExternalizer(): DataExternalizer<XmlInfo> = PluginXmlExternalizer
    override fun getVersion(): Int = 0
    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.fileType == XmlFileType.INSTANCE && file.name == PluginsXml && ProjectLocator.getInstance()
            .getProjectsForFile(file).any { it.allPDEModules().isNotEmpty() }
    }

    private object PluginXmlIndexer : SingleEntryIndexer<XmlInfo>(false) {
        override fun computeValue(inputData: FileContent): XmlInfo? = PluginXmlCacheService.resolvePluginXml(
            inputData.project,
            inputData.file.parent,
            BundleManagementService.getInstance(inputData.project)
                .getBundleByBundleFile(inputData.file.parent)?.sourceBundle?.root,
            inputData.file,
            inputData.content.inputStream()
        )
    }

    private object PluginXmlExternalizer : DataExternalizer<XmlInfo> {
        override fun save(out: DataOutput, value: XmlInfo) {
            out.writeStringList(value.applications)
            out.writeStringList(value.products)

            out.writeInt(value.epPoint2ExsdPath.size)
            value.epPoint2ExsdPath.forEach { (point, file) ->
                out.writeString(point)
                out.writeString(file.fileSystem.protocol)
                out.writeString(file.path)
            }

            out.writeInt(value.epReferenceIdentityMap.size)
            value.epReferenceIdentityMap.forEach { (key, attributes) ->
                out.writeString(key.first)
                out.writeString(key.second)

                out.writeInt(attributes.size)
                attributes.forEach { (attribute, values) ->
                    out.writeString(attribute)
                    out.writeStringList(values)
                }
            }
        }

        override fun read(input: DataInput): XmlInfo {
            val applications = input.readStringList().toHashSet()
            val products = input.readStringList().toHashSet()
            val epPoint2ExsdPath = (0 until input.readInt()).map {
                input.readString() to VirtualFileManager.getInstance().getFileSystem(input.readString())
                    .findFileByPath(input.readString())
            }.filterNot { it.second == null }.associate { it.first to it.second!! }.toMap(hashMapOf())
            val epReferenceIdentityMap = (0 until input.readInt()).associate {
                input.readString() to input.readString() to (0 until input.readInt()).associate {
                    input.readString() to input.readStringList().toHashSet()
                }.toMap(hashMapOf())
            }.toMap(hashMapOf())

            return XmlInfo(applications, products, epPoint2ExsdPath, epReferenceIdentityMap)
        }
    }
}
