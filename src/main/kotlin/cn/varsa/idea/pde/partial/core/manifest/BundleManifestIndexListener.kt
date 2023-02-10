package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.BundleManifest
import com.intellij.openapi.vfs.VirtualFile
import java.util.*

fun interface BundleManifestIndexListener : EventListener {
  fun manifestUpdated(file: VirtualFile, manifest: BundleManifest)
}