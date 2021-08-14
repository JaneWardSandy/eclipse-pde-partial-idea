package cn.varsa.idea.pde.partial.plugin.openapi.provider

import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.application.*
import java.io.*

class PdeBundleProviderRegistry {

    companion object {
        val instance: PdeBundleProviderRegistry
            get() = ApplicationManager.getApplication().getService(PdeBundleProviderRegistry::class.java)
    }

    fun resolveLocation(rootDirectory: File, location: TargetLocationDefinition, processBundle: (File) -> Unit) =
        TargetBundleProvider.EP_NAME.extensionList.forEach {
            if (it.resolveDirectory(rootDirectory, processBundle)) {
                location.type = it.type
                return
            }
        }
}
