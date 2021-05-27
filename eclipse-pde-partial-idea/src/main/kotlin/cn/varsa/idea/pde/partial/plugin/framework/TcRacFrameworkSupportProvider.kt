package cn.varsa.idea.pde.partial.plugin.framework

import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.facet.ui.*
import com.intellij.ide.util.frameworkSupport.*
import com.intellij.openapi.roots.*

class TcRacFrameworkSupportProvider : FacetBasedFrameworkSupportProvider<PDEFacet>(PDEFacetType.getInstance()) {
    override fun setupConfiguration(facet: PDEFacet, rootModel: ModifiableRootModel, version: FrameworkVersion?) {}
}
