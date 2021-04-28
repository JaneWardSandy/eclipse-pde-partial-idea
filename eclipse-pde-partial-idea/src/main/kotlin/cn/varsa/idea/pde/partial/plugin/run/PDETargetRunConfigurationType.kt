package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.execution.configurations.*
import com.intellij.icons.*
import javax.swing.*

class PDETargetRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = message("run.local.configType.displayName")
    override fun getConfigurationTypeDescription(): String = message("run.local.configType.description")
    override fun getIcon(): Icon = AllIcons.Providers.Eclipse
    override fun getId(): String = "cn.varsa.idea.pde.partial.plugin.run.PDETargetRunConfigurationType"
    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(PDETargetRunConfigurationFactory(this))
}
