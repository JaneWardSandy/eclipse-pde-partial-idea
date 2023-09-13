package cn.varsa.idea.pde.partial.core.facet

import cn.varsa.idea.pde.partial.core.facet.PDEFacet.Companion.FACET_ID
import cn.varsa.idea.pde.partial.core.facet.PDEFacet.Companion.FACET_NAME
import cn.varsa.idea.pde.partial.core.facet.PDEFacet.Companion.FACET_TYPE_ID
import com.intellij.facet.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.*
import javax.swing.Icon

class PDEFacetType : FacetType<PDEFacet, PDEFacetConfiguration>(FACET_TYPE_ID, FACET_ID, FACET_NAME) {

  override fun createDefaultConfiguration(): PDEFacetConfiguration = PDEFacetConfiguration()

  override fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean = moduleType is JavaModuleType

  override fun createFacet(
    module: Module,
    name: String,
    configuration: PDEFacetConfiguration,
    underlyingFacet: Facet<*>?,
  ): PDEFacet = PDEFacet(this, module, name, configuration, underlyingFacet)

  override fun getIcon(): Icon = AllIcons.Providers.Eclipse
}