package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import org.osgi.framework.*
import org.osgi.framework.Constants.*

fun BundleManifest.getExportedPackageName(packageName: String): String? =
    exportPackage?.keys?.map { it.substringBefore(".*") }?.firstOrNull { packageName == it }

fun BundleManifest.isPackageImported(packageName: String, version: Set<Version> = emptySet()): Boolean =
    importPackage?.filterKeys { packageName == it }?.let { map ->
        if (version.isEmpty()) map.isNotEmpty()
        else map.values.map { it.attribute[VERSION_ATTRIBUTE].parseVersionRange() }
            .any { range -> version.any { range.includes(it) } }
    } == true

fun BundleManifest.isBundleRequired(symbolicName: String, version: Set<Version> = emptySet()): Boolean =
    requireBundle?.filterKeys { symbolicName == it }?.let { map ->
        if (version.isEmpty()) map.isNotEmpty()
        else map.values.map { it.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }
            .any { range -> version.any { range.includes(it) } }
    } == true

fun BundleManifest.requiredBundleAndVersion(): Map<String, VersionRange> =
    requireBundle?.mapValues { (_, attrs) -> attrs.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }
        ?: emptyMap()

fun BundleManifest.reexportRequiredBundleAndVersion(): Map<String, VersionRange> =
    requireBundle?.filter { it.value.directive[VISIBILITY_DIRECTIVE] == VISIBILITY_REEXPORT }
        ?.mapValues { (_, attrs) -> attrs.attribute[BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() } ?: emptyMap()

fun BundleManifest.importedPackageAndVersion(): Map<String, VersionRange> =
    importPackage?.mapValues { (_, attrs) -> attrs.attribute[VERSION_ATTRIBUTE].parseVersionRange() } ?: emptyMap()

fun BundleManifest.exportedPackageAndVersion(): Map<String, Version> =
    exportPackage?.mapKeys { it.key.substringBefore(".*") }
        ?.mapValues { (_, attrs) -> attrs.attribute[VERSION_ATTRIBUTE].let { Version.parseVersion(it) } } ?: emptyMap()
