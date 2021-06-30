package cn.varsa.idea.pde.partial.plugin.openapi

import com.intellij.openapi.extensions.*
import com.intellij.openapi.module.*

interface BuildLibraryResolver : LibraryResolver<Module> {
    companion object EPs {
        val EP_NAME =
            ExtensionPointName.create<BuildLibraryResolver>("cn.varsa.idea.eclipse.pde.partial.buildLibraryResolver")
    }
}
