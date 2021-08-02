package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import org.osgi.framework.Constants.*

val BundleManifest.reExportRequiredBundleSymbolNames: Set<String>
    get() = requireBundle?.filter { it.value.directive[VISIBILITY_DIRECTIVE] == VISIBILITY_REEXPORT }?.keys
        ?: emptySet()

fun BundleManifest.getExportedPackage(packageName: String): String? =
    exportPackage?.keys?.map { it.substringBefore(".*") }?.firstOrNull { packageName == it }

fun BundleManifest.isPackageImported(packageName: String): Boolean =
    importPackage?.keys?.any { packageName == it } == true

fun BundleManifest.isBundleRequired(symbolicName: String): Boolean = requireBundle?.keys?.contains(symbolicName) == true
