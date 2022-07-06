package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.project.*

class ProjectLibraryResetOperation : TargetDefinitionChangeListener {
  override fun locationsChanged(project: Project) {
    BundleManagementService.getInstance(project).backgroundResolve(project)
  }
}
