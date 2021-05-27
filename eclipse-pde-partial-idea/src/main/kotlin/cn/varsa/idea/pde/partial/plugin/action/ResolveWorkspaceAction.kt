package cn.varsa.idea.pde.partial.plugin.action

import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.actionSystem.*

class ResolveWorkspaceAction : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            text = message("action.resolveWorkspace.text")
            isEnabledAndVisible = e.project?.allPDEModules()?.isNotEmpty() ?: false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.also { TargetDefinitionService.getInstance(it).backgroundResolve(it) }
    }
}
