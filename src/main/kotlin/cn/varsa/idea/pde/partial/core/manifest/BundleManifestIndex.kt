package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.manifest.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.progress.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import com.jetbrains.rd.util.*
import org.jetbrains.kotlin.idea.core.util.*
import org.jetbrains.lang.manifest.*
import java.io.*
import java.util.jar.*

object BundleManifestIndex : SingleEntryFileBasedIndexExtension<BundleManifest>() {
  val KEY: ID<Int, BundleManifest> = ID.create(BundleManifestIndex::class.java.canonicalName)
  private val LOG = thisLogger()

  override fun getName(): ID<Int, BundleManifest> = KEY
  override fun dependsOnFileContent(): Boolean = true
  override fun getVersion(): Int = 2
  override fun getIndexer(): SingleEntryIndexer<BundleManifest> = INDEXER
  override fun getValueExternalizer(): DataExternalizer<BundleManifest> = Externalizer
  override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { virtualFile ->
    virtualFile.isInLocalFileSystem && virtualFile.fileType is ManifestFileType && virtualFile.name == Constants.Partial.File.MANIFEST_MF
  }

  private val INDEXER = object : SingleEntryIndexer<BundleManifest>(false) {
    override fun computeValue(inputData: FileContent): BundleManifest? = try {
      if (inputData.fileType is ManifestFileType) {
        val manifest = Manifest(inputData.content.inputStream())
        BundleManifest(manifest)
      } else null
    } catch (e: ProcessCanceledException) {
      throw e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      LOG.warn("Error while indexing file ${inputData.fileName}: ${e.message}")
      null
    }
  }

  private val Externalizer = object : DataExternalizer<BundleManifest> {
    override fun save(out: DataOutput, manifest: BundleManifest?) {
      out.writeBoolean(manifest == null)
      if (manifest != null) {
        out.writeInt(manifest.attribute.size)
        for ((key, value) in manifest.attribute) {
          out.writeString(key)
          out.writeString(value)
        }
      }
    }

    override fun read(`in`: DataInput): BundleManifest? = if (`in`.readBoolean()) null
    else {
      val map = (0 until `in`.readInt()).associate { `in`.readString() to `in`.readString() }
      BundleManifest(map)
    }
  }
}
