package cn.varsa.idea.pde.partial.plugin.run

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.*

class PDETargetRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        PDETargetRunConfiguration(project, this, message("run.local.config.displayName"))

    override fun getName(): String = message("run.local.configFactory.displayName")
    override fun getId(): String = "cn.varsa.idea.pde.partial.plugin.run.PDETargetRunConfigurationFactory"
}
