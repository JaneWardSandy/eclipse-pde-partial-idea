package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.module.*

class ModuleSetupOperation : FacetChangeListener {

  override fun compileOutputRelativePathChanged(module: Module, oldValue: String, newValue: String) {
    val facet = PDEFacet.getInstance(module) ?: return
    ModuleHelper.setCompileOutputPath(
      module,
      "${module.getModuleDir()}/$newValue",
      "${module.getModuleDir()}/${facet.configuration.compilerTestClassesOutput}"
    )
  }

  override fun compileTestOutputRelativePathChanged(module: Module, oldValue: String, newValue: String) {
    val facet = PDEFacet.getInstance(module) ?: return
    ModuleHelper.setCompileOutputPath(
      module,
      "${module.getModuleDir()}/${facet.configuration.compilerClassesOutput}",
      "${module.getModuleDir()}/$newValue"
    )
  }

  override fun binaryOutputChanged(module: Module, oldChecked: Set<String>, newChecked: Set<String>) {
    ModuleHelper.setCompileArtifact(module, newChecked - oldChecked, oldChecked - newChecked)
  }
}
