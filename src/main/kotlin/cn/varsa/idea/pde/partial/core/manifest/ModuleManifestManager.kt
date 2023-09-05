package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import cn.varsa.idea.pde.partial.common.version.VersionRange
import cn.varsa.idea.pde.partial.core.extension.*
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.ProjectTopics
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.update.*

class ModuleManifestManager(private val module: Module) : Disposable, ModuleRootListener, ModuleListener,
                                                          BundleManifestManager.ManifestIndexedListener,
                                                          DumbService.DumbModeListener {
  companion object {
    private val logger = thisLogger()

    fun getInstance(module: Module): ModuleManifestManager = module.getService(ModuleManifestManager::class.java)
  }

  private val bundleManifestManager = BundleManifestManager.getInstance(module.project)
  private val _visibleBundles = mutableMapOf<String, VersionRange>()
  private val _exportedBundles = mutableMapOf<String, VersionRange>()

  var manifest: BundleManifest? = null
    private set

  /**
   * All visible dependencies for the current module, excluding export dependencies provided by other modules.
   */
  val visibleBundles: Map<String, VersionRange> = _visibleBundles

  /**
   * All re-export required bundles, including exported by its dependencies, excluding export dependencies provided by other modules.
   */
  val exportedBundles: Map<String, VersionRange> = _exportedBundles

  private val calculationUpdate = DisposableUpdate.createDisposable(this, "calculation") { doUpdateDependencies() }
  private val calculationQueue = MergingUpdateQueue(
    "ModuleManifestCalculationQueue",
    500,
    true,
    null,
    this,
    null,
    false,
  )

  init {
    val busConnection = module.project.messageBus.connect()
    busConnection.subscribe(ProjectTopics.PROJECT_ROOTS, this)
    busConnection.subscribe(ProjectTopics.MODULES, this)

    bundleManifestManager.addListener(this, this)
  }

  override fun dispose() {
    manifest = null
    _visibleBundles.clear()
    _exportedBundles.clear()
  }

  override fun enteredDumbMode() = calculationQueue.suspend()
  override fun exitDumbMode() = calculationQueue.resume()

  override fun rootsChanged(event: ModuleRootEvent) = updateDependencies()
  override fun modulesAdded(project: Project, modules: MutableList<out Module>) = updateDependencies()
  override fun moduleRemoved(project: Project, module: Module) = updateDependencies()
  override fun manifestUpdated(manifests: Collection<BundleManifest>) = updateDependencies()
  override fun moduleManifestUpdated(module: Module, file: VirtualFile, manifest: BundleManifest) {
    if (module == this.module) {
      this.manifest = manifest
      updateDependencies()
    }
  }

  private fun updateDependencies() {
    calculationQueue.queue(calculationUpdate)
  }

  private fun doUpdateDependencies() {
    val moduleManifest = manifest ?: return
    val moduleBSN = moduleManifest.bundleSymbolicName?.key ?: return

    val visible = mutableMapOf<String, VersionRange>()
    val exported = mutableMapOf<String, VersionRange>()

    val required = moduleManifest.requiredBundles()
    if (required != null) {
      val reExported = moduleManifest.requiredBundleBSNs() ?: emptySet()

      val cycleDependency = mutableSetOf<String>()
      val cycleDetector = arrayListOf(moduleBSN)

      fun fillDependencies(bsn: String, range: VersionRange, toExported: Boolean) {
        val index = cycleDetector.indexOf(bsn)
        cycleDetector.add(bsn)

        if (index > -1) {
          cycleDependency += cycleDetector
            .subList(index, cycleDetector.size)
            .joinToString(ManifestBundle.message("detector.cycle.link"))

          return
        }

        try {
          val notInVisible = bsn !in visible
          if (notInVisible) visible[bsn] = range

          val notInExported = toExported || bsn in reExported && bsn !in exported
          if (notInExported) exported[bsn] = range

          if (notInVisible || notInExported) {
            val manifest = bundleManifestManager.getBundlesByBSN(bsn, range) ?: return
            val bundles = bundleManifestManager.getSelfExportedBundles(manifest) ?: return

            for ((nextBSN, nextRange) in bundles) fillDependencies(nextBSN, nextRange, notInExported)
          }
        } finally {
          cycleDetector.removeLast()
        }
      }
      for ((bsn, range) in required) fillDependencies(bsn, range, bsn in reExported)


      if (cycleDependency.isNotEmpty()) {
        notificationImportant().createNotification(
          ManifestBundle.message("detector.cycle.title"),
          ManifestBundle.message(
            "detector.cycle.leadMessage",
            cycleDependency.joinToString(System.lineSeparator(), System.lineSeparator()),
          ),
          NotificationType.WARNING,
        ).notify(module.project)
      }
    }

    _visibleBundles.clear()
    _exportedBundles.clear()

    _visibleBundles += visible
    _exportedBundles += exported
  }
}