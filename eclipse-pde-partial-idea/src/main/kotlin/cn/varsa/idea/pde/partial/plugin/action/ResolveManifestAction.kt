package cn.varsa.idea.pde.partial.plugin.action

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.*
import com.intellij.openapi.actionSystem.*
import org.jetbrains.kotlin.idea.util.*

class ResolveManifestAction : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            text = EclipsePDEPartialBundles.message("action.resolveManifest.text")
            isEnabledAndVisible = e.project?.run {
                e.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf { it.isInLocalFileSystem && it.name == ManifestMf }
                    ?.findModule(this)?.let { PDEFacet.getInstance(it) != null }
            } == true
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.run {
            e.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf { it.isInLocalFileSystem && it.name == ManifestMf }
                ?.findModule(this)?.also {
                    ModuleHelper.resetCompileOutputPath(it)
                    ModuleHelper.resetCompileArtifact(it)
                    ModuleHelper.resetLibrary(it)
                }
        }
    }
}
