package cn.varsa.idea.pde.partial.plugin.provider

import cn.varsa.idea.pde.partial.common.*
import java.io.*

open class EclipseSDKBundleProvider : DirectoryBundleProvider() {
    override val type: String = "Eclipse SDK"
    override fun resolveDirectory(
        rootDirectory: File, processFeature: (File) -> Unit, processBundle: (File) -> Unit
    ): Boolean {
        File(rootDirectory, Features).takeIf { it.exists() && it.isDirectory }?.listFiles()
            ?.filter { it.isDirectory && !it.isHidden }?.forEach(processFeature)

        super.resolveDirectory(File(rootDirectory, Dropins), processFeature, processBundle)

        val pluginsDirectory = File(rootDirectory, Plugins)
        super.resolveDirectory(pluginsDirectory, processFeature, processBundle)

        return pluginsDirectory.exists()
    }
}
