package cn.varsa.idea.pde.partial.plugin.resolver

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.openapi.resolver.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import com.intellij.util.containers.ContainerUtil.*

class PdeModuleRuntimeLibraryResolver : ManifestLibraryResolver {
    override val displayName: String = message("resolver.pde.moduleRuntime")

    override fun preResolve(area: Module) {
        PDEFacet.getInstance(area) ?: return

        area.updateModel { model ->
            model.orderEntries.filter { it is ModuleOrderEntry || it.presentableName.startsWith(ProjectLibraryNamePrefix) }
                .forEach { model.removeOrderEntry(it) }
        }
    }

    override fun resolve(area: Module) {
        PDEFacet.getInstance(area) ?: return

        val project = area.project
        val cacheService = BundleManifestCacheService.getInstance(project)
        val managementService = BundleManagementService.getInstance(project)
        val bundleManifest = cacheService.getManifest(area) ?: return

        val classesRoot = bundleManifest.bundleClassPath?.keys?.filterNot { it == "." }?.flatMap { binaryName ->
            area.moduleRootManager.contentRoots.mapNotNull { it.findFileByRelativePath(binaryName) }
        }?.map { it.protocolUrl }?.distinct() ?: emptyList()

        area.updateModel { model ->
            val libraryTableModel = model.moduleLibraryTable.modifiableModel

            applicationInvokeAndWait {
                val library = libraryTableModel.getLibraryByName(ModuleLibraryName) ?: writeCompute {
                    libraryTableModel.createLibrary(ModuleLibraryName)
                }

                model.findLibraryOrderEntry(library)?.apply {
                    scope = DependencyScope.COMPILE
                    isExported = true
                }

                val libraryModel = library.modifiableModel

                libraryModel.getUrls(OrderRootType.CLASSES)
                    .forEach { libraryModel.removeRoot(it, OrderRootType.CLASSES) }
                classesRoot.forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }

                writeRun {
                    libraryModel.commit()
                    libraryTableModel.commit()
                }
            }

            val orderedList = bundleManifest.bundleRequiredOrFromReExportOrderedList(project, area)
            val importedList = bundleManifest.importedPackageAndVersion()
            val hostAndRange = bundleManifest.fragmentHostAndVersionRange()

            applicationInvokeAndWait {
                project.allPDEModules(area).filter { module ->
                    val manifest = cacheService.getManifest(module)
                    val bsn = manifest?.bundleSymbolicName?.key
                    val version = manifest?.bundleVersion
                    val range = manifest?.fragmentHostAndVersionRange()
                    (bsn == hostAndRange?.first && version in hostAndRange?.second) || orderedList.any { (it.first == bsn && version == it.second) || (it.first == range?.first && it.second in range.second) } || manifest?.exportedPackageAndVersion()
                        ?.any { (packageName, version) -> version in importedList[packageName] } == true
                }.forEach { model.findModuleOrderEntry(it) ?: model.addModuleOrderEntry(it) }
            }

            project.libraryTable().libraries.filter { it.name?.startsWith(ProjectLibraryNamePrefix) == true }
                .forEach { depLibrary ->
                    depLibrary.name?.substringAfter(ProjectLibraryNamePrefix)
                        ?.let { managementService.getBundleByBCN(it) }?.dependencyScope?.also {
                            (model.findLibraryOrderEntry(depLibrary) ?: model.addLibraryEntry(depLibrary)).apply {
                                scope = it
                                isExported = false
                            }
                        }
                }
        }
    }

    override fun postResolve(area: Module) {
        PDEFacet.getInstance(area) ?: return

        val project = area.project
        val cacheService = BundleManifestCacheService.getInstance(project)
        val manifest = cacheService.getManifest(area) ?: return
        val hostBCN = project.fragmentHostManifest(manifest, area)?.canonicalName

        area.updateModel { model ->
            val orderEntries = model.orderEntries.toMutableList()
            val orderEntriesMap = orderEntries.associateBy { it.presentableName }

            val kotlinOrder = orderEntriesMap.filter { it.key.startsWith(KotlinOrderEntryName) }.values.toSet()
            val runtimeOrder = orderEntriesMap[ModuleLibraryName]
            val hostOrder = orderEntriesMap[hostBCN] ?: orderEntriesMap["$ProjectLibraryNamePrefix$hostBCN"]
            val dependencyOrder =
                manifest.bundleRequiredOrFromReExportOrderedList(project, area).map { it.asCanonicalName }.mapNotNull {
                    orderEntriesMap[it] ?: orderEntriesMap["$ProjectLibraryNamePrefix$it"]
                }

            var libraryIndex = orderEntries.indexOfLast { it is JdkOrderEntry || it is ModuleSourceOrderEntry } + 1
            val arrangeOrderEntries = orderEntries.apply {
                removeAll(kotlinOrder)
                addAll(libraryIndex, kotlinOrder)
                libraryIndex += kotlinOrder.size

                runtimeOrder?.also {
                    remove(it)
                    add(libraryIndex++, it)
                }
                hostOrder?.also {
                    remove(it)
                    add(libraryIndex++, it)
                }

                removeAll(dependencyOrder)
                addAll(libraryIndex, dependencyOrder)
            }.toTypedArray()
            model.rearrangeOrderEntries(arrangeOrderEntries)
        }
    }
}
