package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.domain.*
import com.intellij.psi.*

fun BundleManifest.getExportedPackage(packageName: String): String? =
    exportPackage?.keys?.map { it.substringBefore(".*") }?.firstOrNull { PsiNameHelper.isSubpackageOf(packageName, it) }

fun BundleManifest.isPackageImported(packageName: String): Boolean =
    importPackage?.keys?.any { PsiNameHelper.isSubpackageOf(packageName, it) } == true

fun BundleManifest.isBundleRequired(symbolicName: String): Boolean = requireBundle?.keys?.contains(symbolicName) == true

// FIXME: 2021/4/22
//fun BundleManifest.isBundleRequiredFromReExport(
//    symbolicName: String,
//    cacheService: BundleManifestCacheService,
//): Boolean = requireBundle?.keys?.mapNotNull { cacheService.libSymbol2ReExportSymbol[it] }
//    ?.flatten()
//    ?.contains(symbolicName) == true
//
//fun BundleManifest.isExportedPackageFromRequiredBundle(
//    packageName: String,
//    cacheService: BundleManifestCacheService,
//): Boolean {
//    requireBundle?.keys?.mapNotNull { cacheService.libSymbol2Manifest[it] }
//        ?.any { it.getExportedPackage(packageName) != null }
//        ?.ifTrue { return true }
//
//    requireBundle?.keys?.mapNotNull { cacheService.libSymbol2ReExportSymbol[it] }
//        ?.flatten()
//        ?.mapNotNull { cacheService.libSymbol2Manifest[it] }
//        ?.any { it.getExportedPackage(packageName) != null }
//        ?.ifTrue { return true }
//
//    return false
//}
