package cn.varsa.idea.pde.partial.plugin.indexes

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import com.jetbrains.rd.util.*
import org.jetbrains.lang.manifest.*
import java.io.*

class BundleManifestIndex : SingleEntryFileBasedIndexExtension<BundleManifest>() {
  companion object {
    val id = ID.create<Int, BundleManifest>("cn.varsa.idea.pde.partial.plugin.indexes.BundleManifestIndex")

    fun readBundleManifest(project: Project, mfFile: VirtualFile): BundleManifest? =
      FileBasedIndex.getInstance().getFileData(id, mfFile, project).firstOrNull()?.value
  }

  override fun getName(): ID<Int, BundleManifest> = id
  override fun getIndexer(): SingleEntryIndexer<BundleManifest> = BundleManifestIndexer
  override fun getValueExternalizer(): DataExternalizer<BundleManifest> = BundleManifestExternalizer
  override fun getVersion(): Int = 0
  override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
    file.fileType == ManifestFileType.INSTANCE && file.name == ManifestMf && ProjectLocator.getInstance()
      .getProjectsForFile(file).any { it?.allPDEModules()?.isNotEmpty() == true }
  }

  private object BundleManifestIndexer : SingleEntryIndexer<BundleManifest>(false) {
    override fun computeValue(inputData: FileContent): BundleManifest? = BundleManifestCacheService.resolveManifest(
      inputData.file, inputData.content.inputStream()
    )
  }

  private object BundleManifestExternalizer : DataExternalizer<BundleManifest> {
    override fun save(out: DataOutput, value: BundleManifest) {
      out.writeInt(value.size)
      value.forEach { (key, value) ->
        out.writeString(key)
        out.writeString(value)
      }
    }

    override fun read(input: DataInput): BundleManifest =
      BundleManifest.parse((0 until input.readInt()).associate { input.readString() to input.readString() })
  }
}
