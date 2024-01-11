package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.module.*

class ModuleSetupOperation : FacetChangeListener {

  override fun compileOutputRelativePathChanged(module: Module, oldValue: String, newValue: String) {
    val facet = PDEFacet.getInstance(module) ?: return
    val moduleDir = module.getModuleDir() ?: return
    ModuleHelper.setCompileOutputPath(
      module,
      facet,
      "$moduleDir/$newValue",
      "$moduleDir/${facet.configuration.compilerTestClassesOutput}"
    )
  }

  override fun compileTestOutputRelativePathChanged(module: Module, oldValue: String, newValue: String) {
    val facet = PDEFacet.getInstance(module) ?: return
    val moduleDir = module.getModuleDir() ?: return
    ModuleHelper.setCompileOutputPath(
      module,
      facet,
      "$moduleDir/${facet.configuration.compilerClassesOutput}",
      "$moduleDir/$newValue"
    )
  }

  override fun binaryOutputChanged(module: Module, oldChecked: Set<String>, newChecked: Set<String>) {
    val facet = PDEFacet.getInstance(module) ?: return
    ModuleHelper.setCompileArtifact(module, facet, newChecked)
  }
}
