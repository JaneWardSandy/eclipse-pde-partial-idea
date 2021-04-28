package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import com.intellij.openapi.roots.*
import org.jetbrains.kotlin.idea.util.projectStructure.*

fun PDEFacet.toDevModule() = DevModule(
    module.getModuleDir().toFile().toRelativeString(module.project.presentableUrl!!.toFile()),
    BundleManifestCacheService.getInstance(module.project).getManifest(module)?.bundleSymbolicName?.key ?: module.name,
    listOf(
        "${configuration.compilerOutputDirectory}/${CompilerModuleExtension.PRODUCTION}",
        "${configuration.compilerOutputDirectory}/${CompilerModuleExtension.TEST}"
    )
)
