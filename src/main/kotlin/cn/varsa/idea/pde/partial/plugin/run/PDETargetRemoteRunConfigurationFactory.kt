package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.i18n.*
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.*

class PDETargetRemoteRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration = PDETargetRemoteRunConfiguration(
    project, this, EclipsePDEPartialBundles.message("run.remote.config.displayName")
  )

  override fun getName(): String = EclipsePDEPartialBundles.message("run.remote.configFactory.displayName")
  override fun getId(): String = "cn.varsa.idea.pde.partial.plugin.run.PDETargetRemoteRunConfigurationFactory"
}
