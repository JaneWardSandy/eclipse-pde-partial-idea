package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
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

  private val calculationUpdate = object : Update("") {
    override fun run() = doRunCalculation()
  }

  private val calculationToProcess = mutableSetOf<BundleManifest>()
  private val calculationQueue = MergingUpdateQueue("", 500, true, null, this, null, false)
  private val mutex = ReentrantLock()
  private val manifestData = BundleManifestData()

  init {
    BundleManifestIndex.getInstance()?.addListener(this, this)
    project.messageBus.connect().subscribe(DumbService.DUMB_MODE, this)
  }

  override fun dispose() {
    // current nothing to dispose
  }

  override fun manifestUpdated(file: VirtualFile, manifest: BundleManifest) {
    mutex.withLock { calculationToProcess.add(manifest) }
    calculationQueue.queue(calculationUpdate)
  }

  override fun enteredDumbMode() = calculationQueue.suspend()
  override fun exitDumbMode() = calculationQueue.resume()

  private fun doRunCalculation() {
    if (LightEdit.owns(project)) return

    val calculationToProcess = mutex.withLock {
      val set = calculationToProcess.toSet()
      calculationToProcess.clear()
      set
    }
    if (calculationToProcess.isEmpty()) return

    logger.debug { "Starting manifest dependencies calculation: $calculationToProcess" }
    for (manifest in calculationToProcess) manifestData.updateManifest(manifest)
  }

  // todo 2023/01/04: bundle removed
}