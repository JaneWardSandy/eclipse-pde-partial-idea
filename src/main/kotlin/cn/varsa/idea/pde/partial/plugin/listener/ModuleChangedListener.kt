package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.common.ArtifactPrefix
import cn.varsa.idea.pde.partial.plugin.facet.PDEFacet
import cn.varsa.idea.pde.partial.plugin.helper.ModuleHelper
import cn.varsa.idea.pde.partial.plugin.support.applicationInvokeAndWait
import cn.varsa.idea.pde.partial.plugin.support.readCompute
import cn.varsa.idea.pde.partial.plugin.support.writeRun
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.util.Function

class ModuleChangedListener : ModuleListener {
  override fun moduleRemoved(project: Project, module: Module) {
    val facet = PDEFacet.getInstance(module) ?: return
    if (!facet.configuration.updateArtifacts) return

    val model = readCompute { ArtifactManager.getInstance(project).createModifiableModel() }
    try {
      model.findArtifact("$ArtifactPrefix${module.name}")?.also { model.removeArtifact(it) }

      applicationInvokeAndWait { if (!project.isDisposed) writeRun(model::commit) }
    } finally {
      model.dispose()
    }
  }

  override fun modulesRenamed(
    project: Project, modules: MutableList<out Module>, oldNameProvider: Function<in Module, String>
  ) {
    modules.mapNotNull {
      val facet = PDEFacet.getInstance(it)
      if (facet != null && facet.configuration.updateArtifacts) facet to it else null
    }.forEach { (facet, module) ->
      val model = readCompute { ArtifactManager.getInstance(project).createModifiableModel() }
      try {
        model.findArtifact("$ArtifactPrefix${oldNameProvider.`fun`(module)}")?.also { model.removeArtifact(it) }

        applicationInvokeAndWait { if (!project.isDisposed) writeRun(model::commit) }
      } finally {
        model.dispose()
      }

      ModuleHelper.resetCompileArtifact(module)
    }
  }
}
