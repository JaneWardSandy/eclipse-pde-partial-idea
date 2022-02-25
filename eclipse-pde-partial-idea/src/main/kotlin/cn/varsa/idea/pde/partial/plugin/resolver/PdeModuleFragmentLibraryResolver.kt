package cn.varsa.idea.pde.partial.plugin.resolver

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.i18n.*
import cn.varsa.idea.pde.partial.plugin.openapi.resolver.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*

class PdeModuleFragmentLibraryResolver : ManifestLibraryResolver {
    override val displayName: String = EclipsePDEPartialBundles.message("resolver.pde.moduleFragment")

    override fun resolve(area: Module) {
        PDEFacet.getInstance(area) ?: return

        val cacheService = BundleManifestCacheService.getInstance(area.project)
        val hostAndRange = cacheService.getManifest(area)?.fragmentHostAndVersionRange()

        area.updateModel { model ->
            area.project.allPDEModules(area).filter {
                cacheService.getManifest(it)?.run {
                    bundleSymbolicName?.key == hostAndRange?.first && bundleVersion in hostAndRange?.second
                } == true
            }.forEach { model.findModuleOrderEntry(it) ?: model.addModuleOrderEntry(it) }
        }
    }

    override fun postResolve(area: Module) {
        PDEFacet.getInstance(area) ?: return

        val project = area.project
        val cacheService = BundleManifestCacheService.getInstance(project)
        val managementService = BundleManagementService.getInstance(project)

        val manifest = cacheService.getManifest(area) ?: return

        val moduleMap = project.allPDEModules(area).mapNotNull { cacheService.getManifest(it) }
            .associateBy { it.bundleSymbolicName?.key }

        val fragment2Host = moduleMap.mapValues { (_, v) ->
            v.fragmentHostAndVersionRange()?.let { r ->
                moduleMap[r.first]?.takeIf { it.bundleVersion in r.second }?.bundleSymbolicName?.key
                    ?: managementService.getBundlesByBSN(r.first, r.second)?.canonicalName
            }
        } + managementService.getBundles().mapNotNull { it.manifest }
            .associate { it.canonicalName to it.fragmentHostAndVersionRange() }.filterValues { it != null }
            .mapValues { (_, r) -> r!! }.mapValues { (_, r) ->
                moduleMap[r.first]?.takeIf { it.bundleVersion in r.second }?.bundleSymbolicName?.key
                    ?: managementService.getBundlesByBSN(r.first, r.second)?.canonicalName
            }

        val hostAndName = manifest.fragmentHostAndVersionRange()?.let { (fragmentHostBSN, fragmentHostVersion) ->
            project.allPDEModules(area).mapNotNull { cacheService.getManifest(it) }
                .firstOrNull { it.isFragmentHost(fragmentHostBSN, fragmentHostVersion) }
                ?.let { it to it.bundleSymbolicName?.key } ?: managementService.getBundlesByBSN(
                fragmentHostBSN, fragmentHostVersion
            )?.manifest?.let { it to it.canonicalName }
        }

        area.updateModel { model ->
            val orderEntries = model.orderEntries.toMutableList()
            val orderEntriesMap = orderEntries.associateBy { it.presentableName }

            val dependencyOrder =
                hostAndName?.first?.bundleRequiredOrFromReExportOrderedList(project, area)?.map { it.asCanonicalName }
                    ?.mapNotNull {
                        orderEntriesMap[it] ?: orderEntriesMap["$ProjectLibraryNamePrefix$it"]
                    }
            val fragment2HostOrder = fragment2Host.map { (fragment, host) ->
                (orderEntriesMap[fragment]
                    ?: orderEntriesMap["$ProjectLibraryNamePrefix$fragment"]) to (orderEntriesMap[host]
                    ?: orderEntriesMap["$ProjectLibraryNamePrefix$host"])
            }.filter { it.first != null && it.second != null }.associate { it.first!! to it.second!! }

            val libraryIndex = orderEntries.indexOfLast {
                it is JdkOrderEntry || it is ModuleSourceOrderEntry || it.presentableName.startsWith(
                    KotlinOrderEntryName
                ) || it.presentableName.equalAny(
                    ModuleLibraryName, "$ProjectLibraryNamePrefix${hostAndName?.second}"
                ) || it.presentableName == hostAndName?.second
            } + 1
            val arrangeOrderEntries = orderEntries.apply {
                dependencyOrder?.also {
                    removeAll(it)
                    addAll(libraryIndex, it)
                }

                fragment2HostOrder.forEach { (fragment, host) ->
                    remove(fragment)
                    add(indexOf(host), fragment)
                }
            }.toTypedArray()
            model.rearrangeOrderEntries(arrangeOrderEntries)
        }
    }
}
