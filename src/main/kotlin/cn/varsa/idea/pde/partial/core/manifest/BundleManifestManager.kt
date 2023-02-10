package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import cn.varsa.idea.pde.partial.common.version.Version
import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BundleManifestManager(private val project: Project) : Disposable, BundleManifestIndexListener,
                                                            DumbService.DumbModeListener {
  companion object {
    private val logger = thisLogger()

    fun getInstance(project: Project): BundleManifestManager = project.getService(BundleManifestManager::class.java)
  }

  private val calculationUpdate = object : Update("calculation") {
    override fun run() = doRunCalculation()
  }

  private val queueMutex = ReentrantLock()
  private val calculationToProcess = hashMapOf<VirtualFile, BundleManifest>()
  private val calculationQueue = MergingUpdateQueue(
    "BundleManifestCalculationQueue",
    500,
    true,
    null,
    this,
    null,
    false,
  )

  private val file2Manifest = hashMapOf<VirtualFile, BundleManifest>()

  /**
   * Bundle-SymbolicName(BSN) to versions
   *
   * It may happen that the BSN and version are the same but in different Bundles, which should not be directly overwritten
   */
  private val bundles = hashMapOf<String, HashMap<Version, HashSet<BundleManifest>>>()

  init {
    BundleManifestIndex.getInstance()?.addListener(this, this)
    project.messageBus.connect().subscribe(DumbService.DUMB_MODE, this)
  }

  override fun dispose() {
    // current nothing to dispose
  }

  override fun manifestUpdated(file: VirtualFile, manifest: BundleManifest) {
    queueMutex.withLock { calculationToProcess[file] = manifest }
    calculationQueue.queue(calculationUpdate)
  }

  override fun enteredDumbMode() = calculationQueue.suspend()
  override fun exitDumbMode() = calculationQueue.resume()

  private fun doRunCalculation() {
    if (LightEdit.owns(project)) return

    val calculationToProcess = queueMutex.withLock {
      val set = calculationToProcess.toMap()
      calculationToProcess.clear()
      set
    }
    if (calculationToProcess.isEmpty()) return

    logger.debug { "Starting manifest dependencies calculation: $calculationToProcess" }
    for ((file, manifest) in calculationToProcess) {
      val bsn = manifest.bundleSymbolicName?.key ?: return
      val version = manifest.bundleVersion

      val oldManifest = file2Manifest[file]
      if (oldManifest != null && (oldManifest.bundleSymbolicName?.key != bsn || oldManifest.bundleVersion != version)) {
        val oldBSN = checkNotNull(oldManifest.bundleSymbolicName?.key) { "BSN should not be null" }

        val versionMap = checkNotNull(bundles[oldBSN]) { "BSN versions should not be null" }
        val manifests = checkNotNull(versionMap[oldManifest.bundleVersion]) { "BSN manifests should not be null" }

        manifests.remove(oldManifest)
        if (manifests.isEmpty()) versionMap.remove(oldManifest.bundleVersion)
        if (versionMap.isEmpty()) bundles.remove(oldBSN)
      }

      file2Manifest[file] = manifest
      bundles.getOrPut(bsn) { hashMapOf() }.getOrPut(version) { hashSetOf() }.add(manifest)
    }
  }

  // todo 2023/01/04: bundle removed
}