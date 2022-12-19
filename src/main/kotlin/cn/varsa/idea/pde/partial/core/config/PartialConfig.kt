package cn.varsa.idea.pde.partial.core.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.*
import com.intellij.util.xmlb.annotations.*

@State(name = "PartialConfig", storages = [Storage("eclipse-pde-partial.xml")])
class PartialConfig : PersistentStateComponent<PartialConfig.State> {

  companion object {
    @JvmStatic fun getInstance(project: Project): PartialConfig = project.getService(PartialConfig::class.java)
  }

  @Volatile private var state: State = State()

  override fun getState(): State = state
  override fun loadState(state: State) {
    this.state = state
  }

  class State {

    @Attribute var targetProvider: String? = "default"

    // TODO: For use in future when we implement the plugin.xml feature
    @XCollection(propertyElementName = "externalEXSDDirectories", style = XCollection.Style.v2)
    var externalEXSDDirectories: MutableList<String> = mutableListOf()
  }
}
