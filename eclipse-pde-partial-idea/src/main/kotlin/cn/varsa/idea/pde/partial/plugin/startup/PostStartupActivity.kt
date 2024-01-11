package cn.varsa.idea.pde.partial.plugin.startup

import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.project.*
import com.intellij.openapi.startup.*

class PostStartupActivity : StartupActivity {
  override fun runActivity(project: Project) {
    val service = TargetDefinitionService.getInstance(project)
    if (service.locations.isNotEmpty()) service.backgroundResolve(project)
  }
}
