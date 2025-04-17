package cn.varsa.idea.pde.partial.plugin.openapi.provider

import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.application.*
import com.intellij.openapi.components.*
import java.io.*

@Service
class PdeBundleProviderRegistry {

  companion object {
    val instance: PdeBundleProviderRegistry
      get() = ApplicationManager.getApplication().getService(PdeBundleProviderRegistry::class.java)
  }

  fun resolveLocation(
    rootDirectory: File,
    location: TargetLocationDefinition,
    processFeature: (File) -> Unit = {},
    processBundle: (File) -> Unit
  ) = TargetBundleProvider.EP_NAME.extensionList.forEach {
    if (it.resolveDirectory(rootDirectory, processFeature, processBundle)) {
      location.type = it.type
      return
    }
  }
}
