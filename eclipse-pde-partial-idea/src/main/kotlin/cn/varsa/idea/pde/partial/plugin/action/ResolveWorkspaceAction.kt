package cn.varsa.idea.pde.partial.plugin.action

import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.openapi.actionSystem.*
import org.jetbrains.kotlin.idea.util.projectStructure.*

class ResolveWorkspaceAction : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            text = message("action.resolveWorkspace.text")
            isEnabledAndVisible =
                e.project?.allModules()?.filter { it.isLoaded }?.any { PDEFacet.getInstance(it) != null } ?: false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.also { ModuleHelper.resetLibrary(it) }?.allModules()?.filter { it.isLoaded }
            ?.filter { PDEFacet.getInstance(it) != null }?.forEach {
                ModuleHelper.resetCompileOutputPath(it)
                ModuleHelper.resetCompileArtifact(it)
                ModuleHelper.resetLibrary(it)
            }
    }
}
