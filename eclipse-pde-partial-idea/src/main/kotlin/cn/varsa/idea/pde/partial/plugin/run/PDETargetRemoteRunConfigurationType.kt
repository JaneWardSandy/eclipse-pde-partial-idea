package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.execution.configurations.*
import com.intellij.icons.*
import javax.swing.*

class PDETargetRemoteRunConfigurationType : ConfigurationType {
  override fun getDisplayName(): String = message("run.remote.configType.displayName")
  override fun getConfigurationTypeDescription(): String = message("run.remote.configType.description")
  override fun getIcon(): Icon = AllIcons.RunConfigurations.RemoteDebug
  override fun getId(): String = "cn.varsa.idea.pde.partial.plugin.run.PDETargetRemoteRunConfigurationType"
  override fun getConfigurationFactories(): Array<ConfigurationFactory> =
    arrayOf(PDETargetRemoteRunConfigurationFactory(this))
}
