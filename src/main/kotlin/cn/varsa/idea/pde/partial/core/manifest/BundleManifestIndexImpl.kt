package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.jetbrains.rd.util.CancellationException
import org.jetbrains.lang.manifest.ManifestFileType
import java.io.*
import java.util.jar.Manifest

class BundleManifestIndexImpl : SingleEntryFileBasedIndexExtension<BundleManifest>() {

  override fun getName(): ID<Int, BundleManifest> = ID.create("BundleManifestIndex")
  override fun dependsOnFileContent(): Boolean = true
  override fun getVersion(): Int = 2
  override fun getIndexer(): SingleEntryIndexer<BundleManifest> = object : SingleEntryIndexer<BundleManifest>(false) {
    override fun computeValue(inputData: FileContent): BundleManifest? = try {
      if (inputData.fileType is ManifestFileType) {
        val manifest = BundleManifest(Manifest(inputData.content.inputStream()))
        if (manifest.bundleSymbolicName?.key.isNullOrBlank()) null
        else manifest
      } else null
    } catch (e: ProcessCanceledException) {
      throw e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      if (e is ControlFlowException) throw e
      logger.warn("Error while indexing file ${inputData.fileName}: ${e.message}")
      null
    }
  }

  override fun getValueExternalizer(): DataExternalizer<BundleManifest> = object : DataExternalizer<BundleManifest> {
    override fun save(output: DataOutput, manifest: BundleManifest?) {
      output.writeBoolean(manifest == null)
      if (manifest != null) {
        output.writeInt(manifest.attribute.size)
        for ((key, value) in manifest.attribute) {
          output.writeString(key)
          output.writeString(value)
        }
      }
    }

    override fun read(input: DataInput): BundleManifest? = if (input.readBoolean()) null else {
      val map = (0 until input.readInt()).associate { input.readString() to input.readString() }
      BundleManifest(map)
    }
  }

  override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { virtualFile ->
    virtualFile.fileType is ManifestFileType && virtualFile.name == Constants.Partial.File.MANIFEST_MF
  }

  companion object {
    private val logger = thisLogger()
  }
}
