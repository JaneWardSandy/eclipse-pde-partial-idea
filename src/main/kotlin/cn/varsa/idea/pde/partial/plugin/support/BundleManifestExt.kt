package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import org.osgi.framework.*
import org.osgi.framework.Constants.*

fun BundleManifest.fragmentHostAndVersionRange() =
  fragmentHost?.run { key to value.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }

val BundleManifest.canonicalName: String get() = "${bundleSymbolicName?.key}-$bundleVersion"

fun BundleManifest.isFragmentHost(
  fragmentHostBSN: String, fragmentHostVersion: VersionRange = VersionRangeAny
): Boolean =
  bundleSymbolicName?.key == fragmentHostBSN && bundleVersion in fragmentHostVersion && fragmentHost?.key.isNullOrBlank()

fun BundleManifest.getExportedPackageName(packageName: String): String? =
  exportPackage?.keys?.map { it.substringBefore(".*") }?.firstOrNull { packageName == it }

fun BundleManifest.isPackageImported(packageName: String, version: Set<Version> = emptySet()): Boolean =
  importPackage?.filterKeys { packageName == it }?.let { map ->
    if (version.isEmpty()) map.isNotEmpty()
    else map.values.map { it.attribute[VERSION_ATTRIBUTE].parseVersionRange() }
      .any { range -> version.any { it in range } }
  } == true

fun BundleManifest.isBundleRequired(symbolicName: String, version: Set<Version> = emptySet()): Boolean =
  requireBundle?.filterKeys { symbolicName == it }?.let { map ->
    if (version.isEmpty()) map.isNotEmpty()
    else map.values.map { it.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }
      .any { range -> version.any { it in range } }
  } == true

fun BundleManifest.requiredBundleAndVersion(): Map<String, VersionRange> =
  requireBundle?.mapValues { (_, attrs) -> attrs.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() } ?: emptyMap()

fun BundleManifest.reexportRequiredBundleAndVersion(): Map<String, VersionRange> =
  requireBundle?.filter { it.value.directive[VISIBILITY_DIRECTIVE] == VISIBILITY_REEXPORT }
    ?.mapValues { (_, attrs) -> attrs.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() } ?: emptyMap()

fun BundleManifest.importedPackageAndVersion(): Map<String, VersionRange> =
  importPackage?.mapValues { (_, attrs) -> attrs.attribute[VERSION_ATTRIBUTE].parseVersionRange() } ?: emptyMap()

fun BundleManifest.exportedPackageAndVersion(): Map<String, Version> =
  exportPackage?.mapKeys { it.key.substringBefore(".*") }
    ?.mapValues { (_, attrs) -> attrs.attribute[VERSION_ATTRIBUTE].let { Version.parseVersion(it) } } ?: emptyMap()

fun BundleManifest.isBundleRequiredOrFromReExport(
  project: Project, module: Module?, symbolName: String, version: Set<Version> = emptySet()
): Boolean {
  val cacheService = BundleManifestCacheService.getInstance(project)
  val managementService = BundleManagementService.getInstance(project)

  // Bundle required directly
  isBundleRequired(symbolName, version).ifTrue { return true }

  val requiredBundle = requireBundle?.keys ?: return false
  val allRequiredFromReExport = managementService.getLibReExportRequired(requiredBundleAndVersion())

  // Re-export dependency tree can resolve bundle
  allRequiredFromReExport.contains(symbolName).ifTrue { return true }

  // Re-export bundle contain module, it needs calc again
  val modulesManifest = project.allPDEModules(module).mapNotNull(cacheService::getManifest).toHashSet()

  modulesManifest.filter {
    it.bundleSymbolicName?.key?.run { requiredBundle.contains(this) || allRequiredFromReExport.contains(this) } == true
  }.any { isBundleFromReExportOnly(it, symbolName, version, cacheService, managementService, modulesManifest) }
    .ifTrue { return true }

  return false
}

private fun isBundleFromReExportOnly(
  manifest: BundleManifest,
  symbolName: String,
  version: Set<Version>,
  cacheService: BundleManifestCacheService,
  managementService: BundleManagementService,
  modulesManifest: HashSet<BundleManifest>
): Boolean {
  // Re-export directly
  manifest.reexportRequiredBundleAndVersion().filterValues { range -> version.isEmpty() || version.any { it in range } }
    .containsKey(symbolName).ifTrue { return true }

  val allReExport = managementService.getLibReExportRequired(manifest.reexportRequiredBundleAndVersion())

  // Re-export dependency tree can resolve bundle
  allReExport.contains(symbolName).ifTrue { return true }

  // Dependency tree contains module, it needs calc again, and remove it from module set to not calc again and again and again
  return modulesManifest.filter { allReExport.contains(it.bundleSymbolicName?.key) }.toSet()
    .also { modulesManifest -= it }
    .any { isBundleFromReExportOnly(it, symbolName, version, cacheService, managementService, modulesManifest) }
}

fun BundleManifest.bundleRequiredOrFromReExportOrderedList(
  project: Project, vararg exclude: Module? = emptyArray()
): LinkedHashSet<Pair<String, Version>> {
  val cacheService = BundleManifestCacheService.getInstance(project)
  val managementService = BundleManagementService.getInstance(project)

  val result = linkedSetOf<Pair<String, Version>>()

  val modulesManifest = project.allPDEModules(*exclude).mapNotNull { cacheService.getManifest(it) }
    .associate { it.bundleSymbolicName?.key to (it.bundleVersion to it) }.toMutableMap()

  fun processBSN(
    exportBundle: String, range: VersionRange, onEach: (Map.Entry<String, VersionRange>) -> Unit
  ) {
    managementService.getBundlesByBSN(exportBundle, range)?.let { result += it.bundleSymbolicName to it.bundleVersion }

    modulesManifest[exportBundle]?.takeIf { it.first in range }
      ?.also { modulesManifest -= exportBundle }?.second?.also { result += exportBundle to it.bundleVersion }
      ?.requiredBundleAndVersion()?.forEach { onEach(it) }
  }

  fun cycleBSN(exportBundle: String, range: VersionRange) {
    processBSN(exportBundle, range) { cycleBSN(it.key, it.value) }

    managementService.getLibReExportRequired(exportBundle, range)?.forEach { (bsn, reqRange) ->
      processBSN(bsn, reqRange) { cycleBSN(it.key, it.value) }
    }
  }

  requiredBundleAndVersion().forEach { cycleBSN(it.key, it.value) }

  return result
}
