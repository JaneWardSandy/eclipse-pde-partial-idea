package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import com.intellij.openapi.project.*

class ProjectLibraryResetOperation : TargetDefinitionChangeListener {
    override fun locationsChanged(
        project: Project,
        changes: Set<Pair<TargetLocationDefinition?, TargetLocationDefinition?>>,
    ) {
        val cacheService = BundleManifestCacheService.getInstance(project)

        cacheService.clearCache()
        ModuleHelper.resetLibrary(project)
        cacheService.buildCache()
    }
}
