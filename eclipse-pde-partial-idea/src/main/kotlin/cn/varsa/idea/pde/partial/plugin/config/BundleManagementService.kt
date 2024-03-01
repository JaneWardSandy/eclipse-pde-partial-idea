package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.config.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.openapi.resolver.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.facet.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import org.jetbrains.kotlin.idea.util.projectStructure.*
import org.osgi.framework.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class BundleManagementService : BackgroundResolvable {
  companion object {
    private val logger = thisLogger()
    fun getInstance(project: Project): BundleManagementService = project.getService(BundleManagementService::class.java)
  }

  private val bundles = hashMapOf<String, HashMap<Version, BundleDefinition>>()
  private val bcn2Bundle = hashMapOf<String, BundleDefinition>()
  private val bundlePath2Bundle = hashMapOf<VirtualFile, BundleDefinition>()
  private val libReExportRequiredSymbolName = hashMapOf<String, HashMap<Version, HashMap<String, VersionRange>>>()
  private val jarPathInnerBundle = hashMapOf<String, BundleDefinition>()

  private fun clear() {
    bundles.clear()
    bcn2Bundle.clear()
    bundlePath2Bundle.clear()
    libReExportRequiredSymbolName.clear()
    jarPathInnerBundle.clear()
  }

  override fun resolve(project: Project, indicator: ProgressIndicator) {
    clear()
    indicator.checkCanceled()
    indicator.text = "Resolving bundle management"
    indicator.isIndeterminate = false
    indicator.fraction = 0.0

    val definitionService = TargetDefinitionService.getInstance(project)

    val bundleStep = 0.45 / (definitionService.locations.sumOf { it.bundles.size } + 1)
    val sourceVersions = hashMapOf<String, HashSet<BundleDefinition>>()

    definitionService.locations.forEach { location ->
      location.bundles.forEach { bundle ->
        indicator.checkCanceled()
        indicator.text2 = "Resolving bundle ${bundle.file}"

        bundle.manifest?.also { manifest ->
          val eclipseSourceBundle = manifest.eclipseSourceBundle
          if (eclipseSourceBundle == null && !location.bundleUnSelected.contains(bundle.canonicalName)) {
            bundles.computeIfAbsent(bundle.bundleSymbolicName) { hashMapOf() } += bundle.bundleVersion to bundle
            bcn2Bundle[bundle.canonicalName] = bundle
            bundlePath2Bundle[bundle.root] = bundle
            bundle.delegateClassPathFile.values.map { it.presentableUrl }.forEach { jarPathInnerBundle[it] = bundle }
          } else if (location.bundleVersionSelection.isEmpty() && eclipseSourceBundle != null) {
            sourceVersions.computeIfAbsent(eclipseSourceBundle.key) { hashSetOf() } += bundle
          }
        }
      }
      indicator.fraction += bundleStep
    }

    val sourceStep = 0.45 / (sourceVersions.size + 1)
    sourceVersions.forEach { (symbolName, sources) ->
      indicator.checkCanceled()
      indicator.text2 = "Resolving source $symbolName"

      bundles[symbolName]?.filter { it.value.sourceBundle == null && it.value.location.bundleVersionSelection.isEmpty() }
        ?.forEach { (version, bundle) ->
          bundle.sourceBundle = sources.firstOrNull { it.bundleVersion == version }
        }

      indicator.fraction += sourceStep
    }

    indicator.checkCanceled()
    indicator.text2 = "Resolving dependency tree"
    indicator.fraction = 0.9

    bundles.mapValues { (_, versionedDefinition) ->
      versionedDefinition.mapValues { (_, bundle) ->
        bundle.manifest?.reexportRequiredBundleAndVersion() ?: emptyMap()
      }
    }.also { l1ReExport ->
      l1ReExport.forEach { (symbolName, exported) ->
        val export = hashMapOf<Version, HashMap<String, VersionRange>>()
        libReExportRequiredSymbolName[symbolName] = export

        exported.forEach { (version, bsn2ver) ->
          val map = hashMapOf<String, VersionRange>()
          export[version] = map
          map += bsn2ver
          fillDependencies(symbolName, map, bsn2ver, l1ReExport)
        }
      }
    }

    indicator.fraction = 1.0
  }

  override fun onFinished(project: Project) {
    ExtensionPointManagementService.getInstance(project).backgroundResolve(project, onFinished = {
      object : BackgroundResolvable {
        override fun resolve(project: Project, indicator: ProgressIndicator) {
          indicator.isIndeterminate = false
          indicator.fraction = 0.0

          indicator.checkCanceled()
          indicator.text = "Rebuild project settings"

          indicator.text2 = "Clear bundle cache"
          BundleManifestCacheService.getInstance(project).clearCache()
          indicator.fraction = 0.25

          indicator.text2 = "Resolve project library"
          PdeLibraryResolverRegistry.instance.resolveProject(project, indicator)
          indicator.fraction = 0.5

          indicator.text2 = "Reset module settings"
          val allPDEModules = project.allPDEModules()

          project.allModules().forEach { module ->
            val allFacets = FacetManager.getInstance(module).allFacets.joinToString { "${it.typeId}-$it" }
            logger.warn("Module: ${module.name}, In FacetByType: ${module in allPDEModules}, All Facets: $allFacets")
          }

          val definitionService = TargetDefinitionService.getInstance(project)

          logger.warn("All PDE Modules: ${allPDEModules.size}, Locations: ${definitionService.locations.size}, Bundles: ${bundles.size}")
          if (allPDEModules.isNotEmpty() && definitionService.locations.isNotEmpty() && bundles.isNotEmpty()) {
            val step = 0.5 / (allPDEModules.size + 1)
            allPDEModules.forEach {
              indicator.checkCanceled()

              ModuleHelper.resetCompileOutputPath(it)
              ModuleHelper.resetCompileArtifact(it)
              PdeLibraryResolverRegistry.instance.resolveModule(it, indicator)

              indicator.fraction += step
            }
          }

          indicator.fraction = 1.0
        }
      }.backgroundResolve(project)
    })
  }

  private tailrec fun fillDependencies(
    symbolName: String,
    reExport: HashMap<String, VersionRange>,
    next: Map<String, VersionRange>,
    libPair: Map<String, Map<Version, Map<String, VersionRange>>>
  ) {
    val nextSet = next.filterKeys { it != symbolName }
      .mapNotNull { (nextBsn, range) -> libPair[nextBsn]?.filterKeys { it in range }?.values }.flatten()
      .flatMap { it.entries }.associate { it.key to it.value }

    if (!reExport.keys.containsAll(nextSet.keys)) {
      reExport.putAll(nextSet)
      fillDependencies(symbolName, reExport, nextSet, libPair)
    }
  }

  fun getLibReExportRequired(bsn2Rage: Map<String, VersionRange>) =
    bsn2Rage.mapNotNull { (bsn, range) -> getLibReExportRequired(bsn, range) }.flatMap { it.entries }
      .associate { it.key to it.value }

  fun getBundleByBCN(bcn: String) = bcn2Bundle[bcn]
  fun getBundlesByBSN(bsn: String) = bundles[bsn]?.toMap()
  fun getBundlesByBSN(bsn: String, version: Version) = bundles[bsn]?.get(version)
  fun getBundlesByBSN(bsn: String, range: VersionRange) =
    bundles[bsn]?.filterKeys { it in range }?.maxByOrNull { it.key }?.value

  fun getBundles() = bundles.values.flatMap { it.values }
  fun getBundleByInnerJarPath(presentableUrl: String) = jarPathInnerBundle[presentableUrl]
  fun getBundleByBundleFile(bundleRoot: VirtualFile) = bundlePath2Bundle[bundleRoot]

  fun getLibReExportRequired(bsn: String, version: Version) = libReExportRequiredSymbolName[bsn]?.get(version)
  fun getLibReExportRequired(bsn: String, range: VersionRange) =
    libReExportRequiredSymbolName[bsn]?.filterKeys { it in range }?.maxByOrNull { it.key }?.value
}
