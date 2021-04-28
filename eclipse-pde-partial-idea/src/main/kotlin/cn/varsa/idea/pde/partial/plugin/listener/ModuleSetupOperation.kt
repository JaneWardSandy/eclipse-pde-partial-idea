package cn.varsa.idea.pde.partial.plugin.listener

import cn.varsa.idea.pde.partial.plugin.helper.*
import com.intellij.openapi.module.*

class ModuleSetupOperation : FacetChangeListener {

    override fun compileOutputRelativePathChanged(module: Module, oldValue: String, newValue: String) {
        ModuleHelper.setCompileOutputPath(module, newValue)
    }

    override fun binaryOutputChanged(module: Module, oldChecked: Set<String>, newChecked: Set<String>) {
        ModuleHelper.setCompileArtifact(module, newChecked - oldChecked, oldChecked - newChecked)
    }
}
