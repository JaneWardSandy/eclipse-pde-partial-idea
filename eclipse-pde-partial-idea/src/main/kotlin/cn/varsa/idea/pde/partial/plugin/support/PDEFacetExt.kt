package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*

fun PDEFacet.toDevModule() = DevModule(
  module.getModuleDir().toFile().toRelativeString(module.project.presentableUrl!!.toFile()),
  BundleManifestCacheService.getInstance(module.project).getManifest(module)?.bundleSymbolicName?.key ?: module.name,
  listOf(configuration.compilerClassesOutput, configuration.compilerTestClassesOutput)
)
