package cn.varsa.idea.pde.partial.core.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.*
import com.intellij.openapi.components.PersistentStateComponent

class PDEFacetConfiguration : FacetConfiguration, PersistentStateComponent<PDEFacetState> {
  private var facetState = PDEFacetState()

  override fun createEditorTabs(
    editorContext: FacetEditorContext?,
    validatorsManager: FacetValidatorsManager?,
  ): Array<FacetEditorTab> = emptyArray()

  override fun getState(): PDEFacetState = facetState

  override fun loadState(state: PDEFacetState) {
    facetState = state
  }
}