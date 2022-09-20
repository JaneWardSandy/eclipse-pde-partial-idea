package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.manifest.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.progress.*
import com.intellij.util.indexing.*
import com.intellij.util.io.*
import com.jetbrains.rd.util.*
import org.jetbrains.lang.manifest.*
import java.util.jar.*

object BundleManifestNamesIndex : ScalarIndexExtension<String>() {
  val KEY: ID<String, Void> = ID.create(BundleManifestNamesIndex::class.java.canonicalName)
  private val LOG = thisLogger()

  override fun getName(): ID<String, Void> = KEY
  override fun dependsOnFileContent(): Boolean = true
  override fun getVersion(): Int = 2
  override fun getIndexer(): DataIndexer<String, Void, FileContent> = INDEXER
  override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
  override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { virtualFile ->
    virtualFile.isInLocalFileSystem && virtualFile.fileType is ManifestFileType && virtualFile.name == Constants.Partial.File.MANIFEST_MF
  }

  private val INDEXER = DataIndexer<String, Void, FileContent> { inputData ->
    try {
      if (inputData.fileType is ManifestFileType) {
        val manifest = Manifest(inputData.content.inputStream())
        val bsn = manifest.mainAttributes.getValue(Constants.OSGI.Header.BUNDLE_SYMBOLICNAME)
        if (bsn.isNullOrBlank()) return@DataIndexer emptyMap()

        val symbolicName = ManifestParameters.parse(bsn).attributes.firstOrNull()?.key ?: return@DataIndexer emptyMap()

        mapOf(symbolicName to null)
      } else emptyMap()
    } catch (e: ProcessCanceledException) {
      throw e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      LOG.warn("Error while indexing file ${inputData.fileName}: ${e.message}")
      emptyMap()
    }
  }
}
