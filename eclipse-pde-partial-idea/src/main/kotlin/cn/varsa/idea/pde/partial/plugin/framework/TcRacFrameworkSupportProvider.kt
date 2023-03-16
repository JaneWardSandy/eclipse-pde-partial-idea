package cn.varsa.idea.pde.partial.plugin.framework

import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.facet.*
import com.intellij.ide.util.frameworkSupport.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.ui.configuration.*
import javax.swing.*

class TcRacFrameworkSupportProvider :
  FrameworkSupportProvider("facet:cn.varsa.idea.pde.partial.plugin", "Eclipse PDE Partial") {
  private val facetType = PDEFacetType.getInstance()

  override fun getUnderlyingFrameworkId(): String? {
    val typeId = facetType.underlyingFacetType ?: return null
    return "facet:${FacetTypeRegistry.getInstance().findFacetType(typeId).stringId}"
  }

  override fun isEnabledForModuleType(moduleType: ModuleType<*>): Boolean = facetType.isSuitableModuleType(moduleType)
  override fun isSupportAlreadyAdded(module: Module, facetsProvider: FacetsProvider): Boolean =
    facetsProvider.getFacetsByType(module, facetType.id).isNotEmpty()

  override fun getIcon(): Icon = facetType.icon

  override fun createConfigurable(model: FrameworkSupportModel): FrameworkSupportConfigurable =
    TcRacFrameworkConfiguration(this)

  fun addSupport(module: Module, updateArtifacts: Boolean, updateCompilerOutput: Boolean) {
    val facetManager = FacetManager.getInstance(module)
    val model = facetManager.createModifiableModel()

    var underlyingFacet: Facet<*>? = null
    val underlyingFacetType = facetType.underlyingFacetType
    if (underlyingFacetType != null) {
      underlyingFacet = model.getFacetByType(underlyingFacetType)

      thisLogger().assertTrue(underlyingFacet != null, underlyingFacetType)
    }

    val configuration = ProjectFacetManager.getInstance(module.project).createDefaultConfiguration(facetType)
    configuration.updateArtifacts = updateArtifacts
    configuration.updateCompilerOutput = updateCompilerOutput

    val facet = facetManager.createFacet(facetType, facetType.defaultFacetName, configuration, underlyingFacet)
    model.addFacet(facet)
    model.commit()
  }
}
