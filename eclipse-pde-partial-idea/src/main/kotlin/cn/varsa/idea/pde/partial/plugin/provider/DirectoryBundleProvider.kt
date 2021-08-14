package cn.varsa.idea.pde.partial.plugin.provider

import cn.varsa.idea.pde.partial.plugin.openapi.provider.*
import java.io.*

open class DirectoryBundleProvider : TargetBundleProvider {
    override val type: String = "Directory"
    override fun resolveDirectory(rootDirectory: File, processBundle: (File) -> Unit): Boolean {
        rootDirectory.takeIf { it.exists() && it.isDirectory }?.listFiles()?.filterNot { it.isHidden }
            ?.forEach(processBundle)
        return true
    }
}
