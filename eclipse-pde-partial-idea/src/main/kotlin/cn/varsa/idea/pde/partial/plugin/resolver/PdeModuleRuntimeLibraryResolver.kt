package cn.varsa.idea.pde.partial.plugin.resolver

import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.openapi.*
import com.intellij.openapi.module.*
import com.intellij.openapi.progress.*

class PdeModuleRuntimeLibraryResolver : ManifestLibraryResolver {
    override val displayName: String = message("resolver.pde.moduleRuntime")
    override fun resolve(area: Module, indicator: ProgressIndicator) {
        ModuleHelper.resetLibrary(area)
    }
}
