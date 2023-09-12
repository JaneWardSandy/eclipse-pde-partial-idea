package cn.varsa.idea.pde.partial.manifest.lang

import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.version.VersionRange
import cn.varsa.idea.pde.partial.core.manifest.BundleManifestIndex
import cn.varsa.idea.pde.partial.manifest.psi.ManifestHeaderPart
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import org.jetbrains.annotations.Nls

object CommonManifestHeaderParser {

  fun checkManifestWithBundleVersionRange(
    bundleSymbolicName: String,
    clause: ManifestHeaderPart.Clause,
    holder: AnnotationHolder,
    weakWarning: Boolean = false,
    @Nls targetWasFragmentMessage: () -> String,
  ): Boolean {
    val manifests = BundleManifestIndex.getManifestBySymbolicName(bundleSymbolicName, clause.project).values
    if (manifests.isEmpty()) {
      holder.newAnnotation(
        if (weakWarning) HighlightSeverity.WEAK_WARNING else HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.bundleNotExists", bundleSymbolicName)
      ).range(clause.getValue()?.highlightingRange ?: clause.textRange).create()
      return true
    }

    val versionAttribute = clause.getAttributes().firstOrNull { it.name == BUNDLE_VERSION_ATTRIBUTE }?.getValueElement()
    val versionRange = try {
      versionAttribute?.unwrappedText.parseVersionRange()
    } catch (e: Exception) {
      VersionRange.ANY_VERSION_RANGE
    }
    val manifest = manifests.sortedByDescending { it.bundleVersion }.firstOrNull { it.bundleVersion in versionRange }
    if (manifest == null) {
      val versions = manifests.map { it.bundleVersion }.sorted()
      holder.newAnnotation(
        if (weakWarning) HighlightSeverity.WEAK_WARNING else HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.notExistVersionInRange", versionRange, versions.joinToString())
      ).range(versionAttribute?.highlightingRange ?: clause.getValue()?.highlightingRange ?: clause.textRange).create()
      return true
    } else if (manifest.fragmentHost != null) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, targetWasFragmentMessage())
        .range(clause.getValue()?.highlightingRange ?: clause.textRange)
        .create()
      return true
    }

    return false
  }

  fun checkVersion(attribute: ManifestHeaderPart.Attribute, holder: AnnotationHolder): Boolean {
    val value = attribute.getValueElement()
    if (value == null || value.unwrappedText.isBlank()) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(value?.highlightingRange ?: attribute.textRange)
        .create()
      return true
    }

    val version = value.unwrappedText
    if (!version.surroundingWith('"')) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.shouldBe", version, "quoted"))
        .range(value.highlightingRange)
        .create()
      return true
    }

    try {
      version.parseVersion()
    } catch (e: Exception) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, e.message ?: e.localizedMessage)
        .range(value.highlightingRange)
        .create()
      return true
    }

    return false
  }

  fun checkVersionRange(attribute: ManifestHeaderPart.Attribute, holder: AnnotationHolder): Boolean {
    val value = attribute.getValueElement()
    if (value == null || value.unwrappedText.isBlank()) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(value?.highlightingRange ?: attribute.textRange)
        .create()
      return true
    }

    val versionRange = value.unwrappedText
    if (!versionRange.surroundingWith('"')) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.shouldBe", versionRange, "quoted")
      ).range(value.highlightingRange).create()
      return true
    }

    try {
      versionRange.parseVersionRange()
    } catch (e: Exception) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, e.message ?: e.localizedMessage)
        .range(value.highlightingRange)
        .create()
      return true
    }

    return false
  }
}