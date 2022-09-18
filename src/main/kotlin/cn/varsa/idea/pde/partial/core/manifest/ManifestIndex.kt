package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.manifest.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.progress.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import org.jetbrains.kotlin.idea.core.util.*
import org.jetbrains.lang.manifest.*
import java.io.*
import java.util.jar.*

object ManifestIndex : SingleEntryFileBasedIndexExtension<BundleManifest>() {
  val KEY: ID<Int, BundleManifest> = ID.create(ManifestIndex::class.java.canonicalName)
  private val LOG = Logger.getInstance(ManifestIndex::class.java)

  override fun getName() = KEY
  override fun dependsOnFileContent() = true
  override fun getVersion() = 2
  override fun getIndexer() = INDEXER
  override fun getValueExternalizer() = Externalizer
  override fun getInputFilter() = FileBasedIndex.InputFilter { virtualFile ->
    virtualFile.fileType is ManifestFileType && virtualFile.name == Constants.Partial.File.MANIFEST_MF
  }

  private val INDEXER = object : SingleEntryIndexer<BundleManifest>(false) {
    override fun computeValue(inputData: FileContent): BundleManifest? = try {
      if (inputData.fileType is ManifestFileType) {
        val manifest = Manifest(ByteArrayInputStream(inputData.content))
        BundleManifest(manifest)
      } else null
    } catch (e: ProcessCanceledException) {
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
