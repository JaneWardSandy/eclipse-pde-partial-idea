package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.extension.readString
import cn.varsa.idea.pde.partial.common.extension.writeString
import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import com.jetbrains.rd.util.CancellationException
import org.jetbrains.lang.manifest.ManifestFileType
import java.io.DataInput
import java.io.DataOutput
import java.util.jar.Manifest

class BundleManifestIndexImpl : FileBasedIndexExtension<String, BundleManifest>() {

  override fun getName(): ID<String, BundleManifest> = BundleManifestIndex.NAME

  override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { virtualFile ->
    virtualFile.fileType is ManifestFileType && virtualFile.name == Constants.Partial.File.MANIFEST_MF
  }

  override fun dependsOnFileContent(): Boolean = true

  override fun getIndexer(): DataIndexer<String, BundleManifest, FileContent> = DataIndexer { inputData ->
    try {
      if (inputData.fileType is ManifestFileType) {
        val manifest = BundleManifest(Manifest(inputData.content.inputStream()))
        val bsn = manifest.bundleSymbolicName?.key
        if (bsn.isNullOrBlank()) emptyMap()
        else mapOf(bsn to manifest)
      } else emptyMap()
    } catch (e: ProcessCanceledException) {
      throw e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      if (e is ControlFlowException) throw e
      logger.warn("Error while indexing file ${inputData.fileName}: ${e.message}")
      emptyMap()
    }
  }

  override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

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

  override fun getVersion(): Int = 2

  companion object {
    private val logger = thisLogger()
  }
}
