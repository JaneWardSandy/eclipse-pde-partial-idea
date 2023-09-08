package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_SYMBOLICNAME_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_OPTIONAL
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.version.*
import cn.varsa.idea.pde.partial.core.manifest.BundleManifestIndex
import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.ManifestHeaderPart
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.CollectionFactory

object ImportPackageParser : BasePackageParser() {

  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    if (super.checkValuePart(clause, holder)) return true

    val value = clause.getValue() ?: return false
    val text = value.unwrappedText

    val versionAttribute = clause.getAttributes().firstOrNull { it.name == VERSION_ATTRIBUTE }?.getValueElement()
    val versionRange = try {
      versionAttribute?.unwrappedText?.parseVersionRange()
    } catch (e: Exception) {
      VersionRange.ANY_VERSION_RANGE
    }

    val optional = clause
      .getDirectives()
      .firstOrNull { it.name == RESOLUTION_DIRECTIVE }
      ?.getValueElement()?.unwrappedText == RESOLUTION_OPTIONAL

    val packageName = text.removePrefix(".*")
    val versions = CollectionFactory.createSmallMemoryFootprintSet<Version?>()
    BundleManifestIndex.processAllManifests(GlobalSearchScope.allScope(clause.project)) { _, manifest ->
      val exports = manifest.exportPackage?.attributes?.filterKeys { it == packageName }

      if (!exports.isNullOrEmpty()) {
        versions += exports.values.map { it.attribute[VERSION_ATTRIBUTE]?.parseVersion() }
      }

      true
    }

    if (versions.isEmpty()) {
      holder.newAnnotation(
        if (optional) HighlightSeverity.WEAK_WARNING else HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.nonExportedPackage", packageName)
      ).range(value.highlightingRange).create()
      return true
    }

    if (versionRange != null && versions.none { it != null && it in versionRange }) {
      val list = (if (versions.remove(null)) listOf(ManifestBundle.message("manifest.lang.unset"))
      else emptyList()) + versions.sorted().map { it.toString() }

      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.notExistVersionInRange", versionRange, list.joinToString())
      ).range(versionAttribute?.highlightingRange ?: clause.textRange).create()
      return true
    }

    return false
  }

  override fun checkAttributes(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val attributes = clause.getAttributes()

    val names = attributes.map { it.name }
    if (names.count { it == VERSION_ATTRIBUTE } > 1) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", VERSION_ATTRIBUTE))
        .range(clause.textRange)
        .create()
      return true
    }
    if (names.count { it == BUNDLE_SYMBOLICNAME_ATTRIBUTE } > 1) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", BUNDLE_SYMBOLICNAME_ATTRIBUTE)
      ).range(clause.textRange).create()
      return true
    }

    for (attribute in attributes) {
      when (attribute.name) {
        VERSION_ATTRIBUTE -> if (CommonManifestHeaderParser.checkVersionRange(attribute, holder)) return true

        BUNDLE_SYMBOLICNAME_ATTRIBUTE -> {
          val value = attribute.getValueElement()
          val bundleSymbolicName = value?.unwrappedText

          if (bundleSymbolicName.isNullOrBlank()) {
            holder
              .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
              .range(value?.highlightingRange ?: attribute.textRange)
              .create()
            return true
          }

          val manifests = BundleManifestIndex.getAllManifestBySymbolicNames(setOf(bundleSymbolicName), clause.project)
          if (manifests.isEmpty()) {
            holder.newAnnotation(
              HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.bundleNotExists", bundleSymbolicName)
            ).range(value.highlightingRange).create()
            return true
          }
        }

        else -> {
          holder.newAnnotation(
            HighlightSeverity.ERROR, ManifestBundle.message(
              "manifest.lang.specifyOnly", "$VERSION_ATTRIBUTE/$BUNDLE_SYMBOLICNAME_ATTRIBUTE", "Attributes"
            )
          ).range(attribute.textRange).create()
          return true
        }
      }
    }

    return false
  }

  override fun checkDirectives(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val directives = clause.getDirectives()
    if (directives.isEmpty()) return false

    val directive = directives[0]
    if (directive.name != RESOLUTION_DIRECTIVE) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.specifyOnly", RESOLUTION_DIRECTIVE, "Directives")
      ).range(directive.textRange).create()
      return true
    }

    val value = directive.getValueElement()
    if (value?.unwrappedText != RESOLUTION_OPTIONAL) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.shouldBe", RESOLUTION_DIRECTIVE, RESOLUTION_OPTIONAL)
      ).range(value?.highlightingRange ?: directive.textRange).create()
      return true
    }

    return false
  }
}