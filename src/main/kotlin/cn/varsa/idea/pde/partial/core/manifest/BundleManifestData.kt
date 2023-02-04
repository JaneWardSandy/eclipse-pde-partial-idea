package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import cn.varsa.idea.pde.partial.common.version.Version
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BundleManifestData {
  private val mutex = ReentrantLock()

  /** Bundle-SymbolicName(BSN) to versions  */
  private val bundles = hashMapOf<String, HashMap<Version, BundleManifest>>()

  fun updateManifest(manifest: BundleManifest) {
    val bsn = manifest.bundleSymbolicName?.key ?: return
    mutex.withLock { bundles.getOrPut(bsn) { hashMapOf() } }[manifest.bundleVersion] = manifest
  }
}