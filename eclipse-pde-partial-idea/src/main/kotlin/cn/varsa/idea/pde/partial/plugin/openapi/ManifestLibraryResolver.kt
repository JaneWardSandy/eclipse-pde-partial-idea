package cn.varsa.idea.pde.partial.plugin.openapi

import com.intellij.openapi.extensions.*
import com.intellij.openapi.module.*

interface ManifestLibraryResolver : LibraryResolver<Module> {
    companion object EPs {
        val EP_NAME =
            ExtensionPointName.create<ManifestLibraryResolver>("cn.varsa.idea.eclipse.pde.partial.manifestLibraryResolver")
    }
}
