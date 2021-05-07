package cn.varsa.idea.pde.partial.plugin.startup

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import com.intellij.openapi.project.*
import com.intellij.openapi.startup.*

class PostStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val cacheService = BundleManifestCacheService.getInstance(project)

        TargetDefinitionService.getInstance(project)
            .backgroundResolve(project, onFinished = {
                cacheService.clearCache()
                ModuleHelper.resetLibrary(project)
                cacheService.buildCache()
            })
    }
}
