package cn.varsa.idea.pde.partial.plugin.listener

import com.intellij.openapi.module.*
import com.intellij.util.messages.*
import java.util.*

interface FacetChangeListener : EventListener {
  fun compileOutputRelativePathChanged(module: Module, oldValue: String, newValue: String) {
  }

  fun compileTestOutputRelativePathChanged(module: Module, oldValue: String, newValue: String) {
  }

  fun binaryOutputChanged(module: Module, oldChecked: Set<String>, newChecked: Set<String>) {
  }

  companion object {
    private val topic = Topic("PDE Facet Changes", FacetChangeListener::class.java)

    fun notifyCompileOutputPathChanged(module: Module, oldValue: String, newValue: String) =
      module.project.messageBus.syncPublisher(topic).compileOutputRelativePathChanged(module, oldValue, newValue)

    fun notifyCompileTestOutputPathChanged(module: Module, oldValue: String, newValue: String) =
      module.project.messageBus.syncPublisher(topic).compileTestOutputRelativePathChanged(module, oldValue, newValue)

    fun notifyBinaryOutputChanged(module: Module, oldChecked: Set<String>, newChecked: Set<String>) =
      module.project.messageBus.syncPublisher(topic).binaryOutputChanged(module, oldChecked, newChecked)
  }
}
