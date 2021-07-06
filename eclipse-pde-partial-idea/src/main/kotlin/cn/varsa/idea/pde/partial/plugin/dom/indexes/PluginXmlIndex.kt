package cn.varsa.idea.pde.partial.plugin.dom.indexes

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import com.intellij.ide.highlighter.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import org.jetbrains.kotlin.idea.core.util.*
import java.io.*

class PluginXmlIndex : SingleEntryFileBasedIndexExtension<XmlInfo>() {
    companion object {
        val id = ID.create<Int, XmlInfo>("cn.varsa.idea.pde.partial.plugin.dom.indexes.PluginXmlIndex")

        fun readXmlInfo(project: Project, pluginXml: VirtualFile): XmlInfo? =
            FileBasedIndex.getInstance().getSingleEntryIndexData(id, pluginXml, project)
    }

    override fun getName(): ID<Int, XmlInfo> = id
    override fun getIndexer(): SingleEntryIndexer<XmlInfo> = PluginXmlIndexer
    override fun getValueExternalizer(): DataExternalizer<XmlInfo> = PluginXmlExternalizer
    override fun getVersion(): Int = 0
    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter {
        it.fileType == XmlFileType.INSTANCE && it.name == PluginsXml
    }

    private object PluginXmlIndexer : SingleEntryIndexer<XmlInfo>(false) {
        override fun computeValue(inputData: FileContent): XmlInfo? {
            val project = inputData.project
            val bundleSymbolicName = BundleManifestCacheService.getInstance(project)
                .getManifest(inputData.file.parent)?.bundleSymbolicName?.key ?: return null

            return PluginXmlCacheService.resolvePluginXml(
                bundleSymbolicName, inputData.file, inputData.content.inputStream()
            )
        }
    }

    private object PluginXmlExternalizer : DataExternalizer<XmlInfo> {
        override fun save(out: DataOutput, value: XmlInfo) {
            out.writeStringList(value.applications)
            out.writeStringList(value.products)

            out.writeInt(value.epPoint2ExsdPath.size)
            value.epPoint2ExsdPath.forEach { (point, file) ->
                out.writeUTF(point)
                out.writeUTF(file.presentableUrl)
            }

            out.writeInt(value.epReferenceIdentityMap.size)
            value.epReferenceIdentityMap.forEach { (key, attributes) ->
                out.writeUTF(key.first)
                out.writeUTF(key.second)

                out.writeInt(attributes.size)
                attributes.forEach { (attribute, values) ->
                    out.writeUTF(attribute)
                    out.writeStringList(values)
                }
            }
        }

        override fun read(input: DataInput): XmlInfo {
            val applications = input.readStringList().toHashSet()
            val products = input.readStringList().toHashSet()
            val epPoint2ExsdPath = (0 until input.readInt()).map {
                input.readUTF() to VfsUtil.findFileByIoFile(input.readUTF().toFile(), true)
            }.filterNot { it.second == null }.associate { it.first to it.second!! }.toMap(hashMapOf())
            val epReferenceIdentityMap = (0 until input.readInt()).associate {
                input.readUTF() to input.readUTF() to (0 until input.readInt()).associate {
                    input.readUTF() to input.readStringList().toHashSet()
                }.toMap(hashMapOf())
            }.toMap(hashMapOf())

            return XmlInfo(applications, products, epPoint2ExsdPath, epReferenceIdentityMap)
        }
    }
}
