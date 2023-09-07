package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_SYMBOLICNAME
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_HOST
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.REQUIRE_BUNDLE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_MANDATORY
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_OPTIONAL
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_PRIVATE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_REEXPORT
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.version.VersionRange
import cn.varsa.idea.pde.partial.core.manifest.BundleManifestIndex
import cn.varsa.idea.pde.partial.manifest.lang.BundleManifestHeaderParser
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.lang.manifest.psi.*

object RequireBundleParser : BundleManifestHeaderParser() {

  override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
    for (clause in header.headerValues.mapNotNull { it as? AssignmentExpression.Clause? }) {
      val valuePart = clause.getValue()

      if (valuePart == null || valuePart.unwrappedText.isBlank()) {
        holder
          .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
          .range(valuePart?.highlightingRange ?: clause.textRange)
          .create()
        return true
      }

      if (checkRequired(header, valuePart, clause, holder)) return true
      if (checkAttributes(clause.getAttributes(), valuePart, holder)) return true
      if (checkDirectives(clause.getDirectives(), valuePart, holder)) return true
    }

    return false
  }

  override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> =
    if (headerValuePart.parent is AssignmentExpression.Clause) arrayOf(BundleReference(headerValuePart)) else PsiReference.EMPTY_ARRAY

  private fun checkRequired(
    header: Header,
    valuePart: HeaderValuePart,
    clause: AssignmentExpression.Clause,
    holder: AnnotationHolder,
  ): Boolean {
    val project = header.project
    val bundleSymbolicName = valuePart.unwrappedText
    val headers = PsiTreeUtil.getChildrenOfTypeAsList(header.parent, Header::class.java).associateBy { it.name }

    if (bundleSymbolicName == headers[FRAGMENT_HOST]?.headerValue?.let { it as? HeaderValuePart? }?.unwrappedText) {
      holder.newAnnotation(
        HighlightSeverity.WEAK_WARNING,
        ManifestBundle.message("manifest.lang.requiredWasFragmentHost", bundleSymbolicName)
      ).range(valuePart.highlightingRange).create()
      return true
    }
    if (bundleSymbolicName == headers[BUNDLE_SYMBOLICNAME]?.headerValue?.let { it as? HeaderValuePart? }?.unwrappedText) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.requiredCannotBeSelf"))
        .range(valuePart.highlightingRange)
        .create()
      return true
    }

    val manifests = BundleManifestIndex.getAllManifestBySymbolicNames(setOf(bundleSymbolicName), project).values
    if (manifests.isEmpty()) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.bundleNotExists", bundleSymbolicName)
      ).range(valuePart.highlightingRange).create()
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
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.notExistVersionInRange", versionRange, versions.joinToString())
      ).range(versionAttribute?.highlightingRange ?: valuePart.highlightingRange).create()
      return true
    } else if (manifest.fragmentHost != null) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.requiredCannotBeFragment"))
        .range(valuePart.highlightingRange)
        .create()
      return true
    }

    return false
  }

  private fun checkAttributes(
    attributes: List<AssignmentExpression.Attribute>,
    valuePart: HeaderValuePart,
    holder: AnnotationHolder,
  ): Boolean {
    if (attributes.isEmpty()) return false
    val attribute = attributes[0]

    if (attributes.size > 1 || attribute.name != BUNDLE_VERSION_ATTRIBUTE) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.specifyOnly", BUNDLE_VERSION_ATTRIBUTE, REQUIRE_BUNDLE)
      ).range(attributes.firstOrNull()?.textRange ?: valuePart.highlightingRange).create()
      return true
    }

    val attributeValueElement = attribute.getValueElement()
    if (attributeValueElement == null || attributeValueElement.unwrappedText.isBlank()) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(attributeValueElement?.highlightingRange ?: attribute.textRange)
        .create()
      return true
    }

    val versionRangeText = attributeValueElement.unwrappedText
    try {
      check(versionRangeText.surroundingWith('"')) { "Invalid range \"$versionRangeText\": invalid format, should be quoted" }
      versionRangeText.parseVersionRange()
    } catch (e: Exception) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, e.message ?: e.localizedMessage)
        .range(attributeValueElement.highlightingRange)
        .create()
      return true
    }

    return false
  }

  private fun checkDirectives(
    directives: List<AssignmentExpression.Directive>,
    valuePart: HeaderValuePart,
    holder: AnnotationHolder,
  ): Boolean {
    val names = directives.map { it.name }
    if (names.count { it == VISIBILITY_DIRECTIVE } > 1) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", VISIBILITY_DIRECTIVE))
        .range(valuePart.highlightingRange)
        .create()
      return true
    }
    if (names.count { it == RESOLUTION_DIRECTIVE } > 1) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", RESOLUTION_DIRECTIVE))
        .range(valuePart.highlightingRange)
        .create()
      return true
    }

    for (directive in directives) {
      val valueElement = directive.getValueElement()
      val text = valueElement?.unwrappedText

      val allowValues = when (directive.name) {
        VISIBILITY_DIRECTIVE -> setOf(VISIBILITY_PRIVATE, VISIBILITY_REEXPORT)
        RESOLUTION_DIRECTIVE -> setOf(RESOLUTION_MANDATORY, RESOLUTION_OPTIONAL)
        else -> {
          holder.newAnnotation(
            HighlightSeverity.ERROR, ManifestBundle.message(
              "manifest.lang.specifyOnly", "$VISIBILITY_DIRECTIVE/$RESOLUTION_DIRECTIVE", "Directives"
            )
          ).range(valuePart.highlightingRange).create()
          return true
        }
      }

      if (text.isNullOrBlank() || text !in allowValues) {
        holder.newAnnotation(
          HighlightSeverity.ERROR,
          ManifestBundle.message("manifest.lang.shouldBe", directive.name, allowValues.joinToString("/"))
        ).range(valueElement?.highlightingRange ?: directive.textRange).create()
        return true
      }
    }

    return false
  }
}