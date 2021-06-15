package cn.varsa.idea.pde.partial.plugin.resolver

import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.openapi.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*

class PdeProjectLibraryResolver : TargetPlatformLibraryResolver {
    override val displayName: String = message("resolver.pde.projectLibrary")
    override fun resolve(area: Project, indicator: ProgressIndicator) {
        ModuleHelper.resetLibrary(area)
    }
}
