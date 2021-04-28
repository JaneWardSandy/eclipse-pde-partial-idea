package cn.varsa.idea.pde.partial.plugin.framework

import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import com.intellij.facet.ui.*
import com.intellij.ide.util.frameworkSupport.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*

class TcRacFrameworkSupportProvider : FacetBasedFrameworkSupportProvider<PDEFacet>(PDEFacetType.getInstance()) {

    override fun setupConfiguration(facet: PDEFacet, rootModel: ModifiableRootModel, version: FrameworkVersion?) {
    }

    override fun addSupport(
        module: Module, rootModel: ModifiableRootModel, version: FrameworkVersion?, library: Library?
    ) {
        super.addSupport(module, rootModel, version, library)
        ModuleHelper.setupManifestFile(module)
    }
}
