package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.extension.readString
import cn.varsa.idea.pde.partial.common.extension.writeString
import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.EventDispatcher
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.jetbrains.rd.util.CancellationException
import org.jetbrains.lang.manifest.ManifestFileType
import java.io.DataInput
import java.io.DataOutput
import java.util.jar.Manifest

class BundleManifestIndex : SingleEntryFileBasedIndexExtension<BundleManifest>() {
  companion object {
    private val logger = thisLogger()

    val id: ID<Int, BundleManifest> = ID.create(BundleManifestIndex::class.java.canonicalName)

    fun getInstance() = EXTENSION_POINT_NAME.findExtension(BundleManifestIndex::class.java)
  }

  private val dispatcher = EventDispatcher.create(BundleManifestIndexListener::class.java)

  fun addListener(parentDisposable: Disposable, listener: BundleManifestIndexListener) =
    dispatcher.addListener(listener, parentDisposable)

  override fun getName(): ID<Int, BundleManifest> = id
  override fun dependsOnFileContent(): Boolean = true
  override fun getVersion(): Int = 2
  override fun getIndexer(): SingleEntryIndexer<BundleManifest> = object : SingleEntryIndexer<BundleManifest>(false) {
    override fun computeValue(inputData: FileContent): BundleManifest? = try {
      if (inputData.fileType is ManifestFileType) {
        val manifest = BundleManifest(Manifest(inputData.content.inputStream()))
        dispatcher.multicaster.manifestUpdated(inputData.file, manifest)
        manifest
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
    virtualFile.isInLocalFileSystem && virtualFile.fileType is ManifestFileType && virtualFile.name == Constants.Partial.File.MANIFEST_MF
  }
}
