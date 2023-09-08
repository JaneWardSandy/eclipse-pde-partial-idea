package cn.varsa.idea.pde.partial.core.extension

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.extension.parseVersionRange
import cn.varsa.idea.pde.partial.common.manifest.BundleManifest

/** @see Constants.OSGI.Header.REQUIRE_BUNDLE */
fun BundleManifest.requiredBundleBSNs() =
  requireBundle?.attributes?.filterValues { it.directive[Constants.OSGI.Header.VISIBILITY_DIRECTIVE] == Constants.OSGI.Header.VISIBILITY_REEXPORT }?.keys

/** @see Constants.OSGI.Header.REQUIRE_BUNDLE */
fun BundleManifest.requiredBundles() =
  requireBundle?.attributes?.mapValues { (_, attrs) -> attrs.attribute[Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }

/** @see Constants.OSGI.Header.VISIBILITY_REEXPORT */
fun BundleManifest.reExportRequiredBundles() = requireBundle?.attributes
  ?.filterValues { it.directive[Constants.OSGI.Header.VISIBILITY_DIRECTIVE] == Constants.OSGI.Header.VISIBILITY_REEXPORT }
  ?.mapValues { (_, attrs) -> attrs.attribute[Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE].parseVersionRange() }
