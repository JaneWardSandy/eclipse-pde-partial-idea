package cn.varsa.idea.pde.partial.plugin.facet

import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.openapi.resolver.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.facet.*
import com.intellij.openapi.module.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*

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

    override fun initFacet() {
        ModuleHelper.setupManifestFile(module)
        ModuleHelper.resetCompileOutputPath(module)
        ModuleHelper.resetCompileArtifact(module)
        object : BackgroundResolvable {
            override fun resolve(project: Project, indicator: ProgressIndicator) {
                indicator.checkCanceled()
                indicator.text = "Reset module settings"
                PdeLibraryResolverRegistry.instance.resolveModule(module, indicator)
            }
        }.backgroundResolve(module.project)
    }
}
