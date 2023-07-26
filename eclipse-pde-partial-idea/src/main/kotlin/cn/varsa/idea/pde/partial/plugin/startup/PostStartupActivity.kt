package cn.varsa.idea.pde.partial.plugin.startup

import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.project.*
import com.intellij.openapi.startup.*

class PostStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    TargetDefinitionService.getInstance(project).backgroundResolve(project)
  }
}
