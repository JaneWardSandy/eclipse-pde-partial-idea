package cn.varsa.idea.pde.partial.plugin.indexes

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import org.jetbrains.lang.manifest.*
import java.io.*

class BundleManifestIndex : SingleEntryFileBasedIndexExtension<BundleManifest>() {
    companion object {
        val id = ID.create<Int, BundleManifest>("cn.varsa.idea.pde.partial.plugin.indexes.BundleManifestIndex")

        fun readBundleManifest(project: Project, mfFile: VirtualFile): BundleManifest? =
            FileBasedIndex.getInstance().getSingleEntryIndexData(id, mfFile, project)
    }

    override fun getName(): ID<Int, BundleManifest> = id
    override fun getIndexer(): SingleEntryIndexer<BundleManifest> = BundleManifestIndexer
    override fun getValueExternalizer(): DataExternalizer<BundleManifest> = BundleManifestExternalizer
    override fun getVersion(): Int = 0
    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter {
        it.fileType == ManifestFileType.INSTANCE && it.name == ManifestMf
    }

    private object BundleManifestIndexer : SingleEntryIndexer<BundleManifest>(false) {
        override fun computeValue(inputData: FileContent): BundleManifest? =
            BundleManifestCacheService.resolveManifest(inputData.file, inputData.content.inputStream())
    }

    private object BundleManifestExternalizer : DataExternalizer<BundleManifest> {
        override fun save(out: DataOutput, value: BundleManifest) {
            out.writeInt(value.size)
            value.forEach { (key, value) ->
                out.writeUTF(key)
                out.writeUTF(value)
            }
        }

        override fun read(input: DataInput): BundleManifest =
            BundleManifest.parse((0 until input.readInt()).associate { input.readUTF() to input.readUTF() })
    }
}
