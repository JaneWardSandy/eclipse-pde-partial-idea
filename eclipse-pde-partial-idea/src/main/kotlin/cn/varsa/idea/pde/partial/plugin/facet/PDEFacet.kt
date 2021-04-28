package cn.varsa.idea.pde.partial.plugin.facet

import com.intellij.facet.*
import com.intellij.openapi.module.*

class PDEFacet(
    facetType: FacetType<out Facet<*>, *>,
    module: Module,
    name: String,
    configuration: PDEFacetConfiguration,
    underlyingFacet: Facet<*>?
) : Facet<PDEFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {
    companion object {
        fun getInstance(module: Module): PDEFacet? = FacetManager.getInstance(module).getFacetByType(PDEFacetType.id)
    }
}
