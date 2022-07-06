package cn.varsa.idea.pde.partial.plugin.resolver

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.openapi.resolver.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*

class PdeProjectLibraryResolver : TargetPlatformLibraryResolver {
  override val displayName: String = message("resolver.pde.projectLibrary")

  override fun preResolve(area: Project) {
    if (area.allPDEModules().isEmpty()) return

    val bcn = BundleManagementService.getInstance(area).getBundles().map { it.canonicalName }
    val moduleNames = area.allPDEModulesSymbolicName()

    area.libraryTable().modifiableModel.also { model ->
      model.libraries.filter { library ->
        library.name?.let { name ->
          name.substringAfter(ProjectLibraryNamePrefix, name).let {
            it != name && (moduleNames.contains(it) || !bcn.contains(it))
          }
        } == true
      }.forEach { model.removeLibrary(it) }

      applicationInvokeAndWait { writeRun { model.commit() } }
    }
  }

  override fun resolve(area: Project) {
    val pdeModules = area.allPDEModules()
    if (pdeModules.isEmpty()) return

    val moduleNames = area.allPDEModulesSymbolicName()
    BundleManagementService.getInstance(area).getBundles().filterNot { moduleNames.contains(it.bundleSymbolicName) }
      .also { bundles ->
        val model = LibraryTablesRegistrar.getInstance().getLibraryTable(area).modifiableModel
        val map = hashMapOf<BundleDefinition, Library>()

        applicationInvokeAndWait {
          bundles.forEach { bundle ->
            val libraryName = "$ProjectLibraryNamePrefix${bundle.canonicalName}"
            map[bundle] = model.getLibraryByName(libraryName) ?: writeCompute { model.createLibrary(libraryName) }
          }
        }

        map.map { (bundle, library) ->
          val libraryModel = library.modifiableModel

          libraryModel.getUrls(OrderRootType.CLASSES).forEach { libraryModel.removeRoot(it, OrderRootType.CLASSES) }
          libraryModel.getUrls(OrderRootType.SOURCES).forEach { libraryModel.removeRoot(it, OrderRootType.SOURCES) }

          bundle.delegateClassPathFile.values.map { it.protocolUrl }
            .forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }
          bundle.sourceBundle?.delegateClassPathFile?.values?.map { it.protocolUrl }
            ?.forEach { libraryModel.addRoot(it, OrderRootType.SOURCES) }

          libraryModel
        }.also { list ->
          applicationInvokeAndWait { writeRun { list.forEach { it.commit() } } }
        }

        applicationInvokeAndWait { writeRun { model.commit() } }

        pdeModules.forEach { module ->
          module.updateModel { model ->
            map.forEach { (bundle, library) ->
              (model.findLibraryOrderEntry(library) ?: model.addLibraryEntry(library)).apply {
                scope = bundle.dependencyScope
                isExported = false
              }
            }
          }
        }
      }
  }
}
