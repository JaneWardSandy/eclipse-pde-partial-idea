package cn.varsa.idea.pde.partial.plugin.openapi.provider

import com.intellij.openapi.extensions.*
import java.io.*

interface TargetBundleProvider {
    companion object EPs {
        val EP_NAME =
            ExtensionPointName.create<TargetBundleProvider>("cn.varsa.idea.eclipse.pde.partial.targetBundlesProvider")
    }

    val type: String

    /**
     * @return resolved succeed
     */
    fun resolveDirectory(rootDirectory: File, processBundle: (File) -> Unit): Boolean
}
