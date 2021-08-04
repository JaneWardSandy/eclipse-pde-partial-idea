package cn.varsa.idea.pde.partial.plugin.facet

import com.intellij.facet.*
import com.intellij.icons.*
import com.intellij.openapi.module.*
import javax.swing.*

class PDEFacetType : FacetType<PDEFacet, PDEFacetConfiguration>(id, id.toString(), "Eclipse PDE Partial") {
    companion object {
        val id = FacetTypeId<PDEFacet>("cn.varsa.idea.pde.partial.plugin")
        fun getInstance(): PDEFacetType = findInstance(PDEFacetType::class.java)
    }

    override fun createFacet(
        module: Module, name: String, configuration: PDEFacetConfiguration, underlyingFacet: Facet<*>?
    ): PDEFacet = PDEFacet(this, module, name, configuration, underlyingFacet)

    override fun createDefaultConfiguration(): PDEFacetConfiguration = PDEFacetConfiguration()
    override fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean = moduleType is JavaModuleType
    override fun getIcon(): Icon = AllIcons.Providers.Eclipse
}
