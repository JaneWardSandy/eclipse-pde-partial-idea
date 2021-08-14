package cn.varsa.idea.pde.partial.plugin.provider

import cn.varsa.idea.pde.partial.common.*
import java.io.*

open class EclipseSDKBundleProvider : DirectoryBundleProvider() {
    override val type: String = "Eclipse SDK"
    override fun resolveDirectory(rootDirectory: File, processBundle: (File) -> Unit): Boolean {
        super.resolveDirectory(File(rootDirectory, Dropins), processBundle)

        val pluginsDirectory = File(rootDirectory, Plugins)
        super.resolveDirectory(pluginsDirectory, processBundle)

        return pluginsDirectory.exists()
    }
}
