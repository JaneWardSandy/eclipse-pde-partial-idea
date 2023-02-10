package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import cn.varsa.idea.pde.partial.common.version.Version
import cn.varsa.idea.pde.partial.common.version.VersionRange
import cn.varsa.idea.pde.partial.core.extension.reExportRequiredBundles
import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.update.DisposableUpdate
import com.intellij.util.ui.update.MergingUpdateQueue
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BundleManifestManager(private val project: Project) : Disposable, BundleManifestIndex.ManifestIndexedListener,
                                                            DumbService.DumbModeListener {
  companion object {
    private val logger = thisLogger()

    fun getInstance(project: Project): BundleManifestManager = project.getService(BundleManifestManager::class.java)
  }

  private val projectFileIndex = ProjectFileIndex.getInstance(project)

  private val queueMutex = ReentrantLock()
  private val calculationToProcess = hashMapOf<VirtualFile, BundleManifest>()
  private val calculationUpdate = DisposableUpdate.createDisposable(this, "calculation") { doRunCalculation() }
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
   * Note that the module is not included
   */
  private val bsn2bundles = hashMapOf<String, HashMap<Version, BundleManifest>>()

  /**
   * Manifest to it's re-exported bundles
   *
   * Note that the module is not included
   */
  private val bundle2ReExportBundles = hashMapOf<BundleManifest, Map<String, VersionRange>>()
  private val bundle2Module = hashMapOf<BundleManifest, Module>()

  init {
    BundleManifestIndex.getInstance()?.addListener(this, this)
    project.messageBus.connect().subscribe(DumbService.DUMB_MODE, this)
  }

  override fun dispose() {
    calculationToProcess.clear()
    file2Manifest.clear()
    bsn2bundles.clear()
    bundle2ReExportBundles.clear()
    bundle2Module.clear()
  }

  override fun enteredDumbMode() = calculationQueue.suspend()
  override fun exitDumbMode() = calculationQueue.resume()

  override fun manifestUpdated(file: VirtualFile, manifest: BundleManifest) {
    queueMutex.withLock { calculationToProcess[file] = manifest }
    calculationQueue.queue(calculationUpdate)
  }

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

      val module = projectFileIndex.getModuleForFile(file)
      if (module != null) {
        bundle2Module[manifest] = module
        dispatcher.multicaster.moduleManifestUpdated(module, file, manifest)
      } else {
        val oldManifest = file2Manifest[file]
        if (oldManifest != null) {
          bundle2ReExportBundles -= oldManifest

          if (oldManifest.bundleSymbolicName?.key != bsn || oldManifest.bundleVersion != version) {
            val oldBSN = checkNotNull(oldManifest.bundleSymbolicName?.key) { "BSN should not be null" }

            val versionMap = checkNotNull(bsn2bundles[oldBSN]) { "BSN versions should not be null" }
            val manifests = checkNotNull(versionMap[oldManifest.bundleVersion]) { "BSN manifests should not be null" }

            versionMap -= oldManifest.bundleVersion
            if (versionMap.isEmpty()) bsn2bundles -= oldBSN
          }
        }

        file2Manifest[file] = manifest
        bsn2bundles.getOrPut(bsn) { hashMapOf() }[version] = manifest
        bundle2ReExportBundles[manifest] = manifest.reExportRequiredBundles() ?: emptyMap()
      }
    }

    dispatcher.multicaster.manifestUpdated(calculationToProcess.values)
  }


  fun getBundlesByBSN(bsn: String, version: Version) = bsn2bundles[bsn]?.get(version)
  fun getBundlesByBSN(bsn: String, range: VersionRange) =
    bsn2bundles[bsn]?.filterKeys { it in range }?.maxByOrNull { it.key }?.value

  fun getSelfExportedBundles(manifest: BundleManifest) = bundle2ReExportBundles[manifest]


  private val dispatcher = EventDispatcher.create(ManifestIndexedListener::class.java)
  fun addListener(parentDisposable: Disposable, listener: ManifestIndexedListener) =
    dispatcher.addListener(listener, parentDisposable)

  interface ManifestIndexedListener : EventListener {
    fun moduleManifestUpdated(module: Module, file: VirtualFile, manifest: BundleManifest) {}
    fun manifestUpdated(manifests: Collection<BundleManifest>) {}
  }

  // todo 2023/01/04: bundle removed
}