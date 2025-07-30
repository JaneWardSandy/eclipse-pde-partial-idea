package cn.varsa.idea.pde.partial.core.facet

import com.intellij.facet.*
import com.intellij.openapi.module.*

class PDEFacet(
  facetType: FacetType<out Facet<*>, *>,
  module: Module,
  name: String,
  configuration: PDEFacetConfiguration,
  underlyingFacet: Facet<*>?,
) : Facet<PDEFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {

  companion object {
    const val FACET_ID = "cn.varsa.idea.pde.partial.plugin"
    const val FACET_NAME = "Eclipse PDE Partial"
    val FACET_TYPE_ID = FacetTypeId<PDEFacet>(FACET_ID)
  }
}