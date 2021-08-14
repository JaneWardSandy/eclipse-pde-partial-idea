package cn.varsa.idea.pde.partial.plugin.openapi.resolver

import com.intellij.openapi.extensions.*
import com.intellij.openapi.project.*

interface TargetPlatformLibraryResolver : LibraryResolver<Project> {
    companion object EPs {
        val EP_NAME =
            ExtensionPointName.create<TargetPlatformLibraryResolver>("cn.varsa.idea.eclipse.pde.partial.targetPlatformLibraryResolver")
    }
}
