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
import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.lang.manifest.psi.*

object RequireBundleParser : BundleManifestHeaderParser() {

  override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> =
    if (headerValuePart.parent is ManifestHeaderPart.Clause) arrayOf(BundleReference(headerValuePart)) else PsiReference.EMPTY_ARRAY

  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false

    val requireBundle = value.unwrappedText
    val headers = PsiTreeUtil
      .getChildrenOfTypeAsList(PsiTreeUtil.getParentOfType(clause, Section::class.java), Header::class.java)
      .associateBy { it.name }

    val fragmentHost = headers[FRAGMENT_HOST]?.headerValue?.let { it as? ManifestHeaderPart.Clause? }
    if (requireBundle == fragmentHost?.getValue()?.unwrappedText) {
      holder.newAnnotation(
        HighlightSeverity.WEAK_WARNING, ManifestBundle.message("manifest.lang.requiredWasFragmentHost", requireBundle)
      ).range(value.highlightingRange).create()
      return true
    }

    val bundleSymbolicName = headers[BUNDLE_SYMBOLICNAME]?.headerValue?.let { it as? ManifestHeaderPart.Clause? }
    if (requireBundle == bundleSymbolicName?.getValue()?.unwrappedText) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.requiredCannotBeSelf"))
        .range(value.highlightingRange)
        .create()
      return true
    }

    val optional = clause
      .getDirectives()
      .firstOrNull { it.name == RESOLUTION_DIRECTIVE }
      ?.getValueElement()?.unwrappedText == RESOLUTION_OPTIONAL

    return CommonManifestHeaderParser.checkManifestWithBundleVersionRange(requireBundle, clause, holder, optional) {
      ManifestBundle.message("manifest.lang.requiredCannotBeFragment")
    }
  }

  override fun checkAttributes(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val attributes = clause.getAttributes()
    if (attributes.isEmpty()) return false

    val attribute = attributes[0]
    if (attributes.size > 1 || attribute.name != BUNDLE_VERSION_ATTRIBUTE) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.specifyOnly", BUNDLE_VERSION_ATTRIBUTE, REQUIRE_BUNDLE)
      ).range(attribute.textRange ?: clause.textRange).create()
      return true
    }

    return CommonManifestHeaderParser.checkVersionRange(attribute, holder)
  }

  override fun checkDirectives(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val directives = clause.getDirectives()

    val names = directives.map { it.name }
    if (names.count { it == VISIBILITY_DIRECTIVE } > 1) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", VISIBILITY_DIRECTIVE))
        .range(clause.textRange)
        .create()
      return true
    }
    if (names.count { it == RESOLUTION_DIRECTIVE } > 1) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", RESOLUTION_DIRECTIVE))
        .range(clause.textRange)
        .create()
      return true
    }

    for (directive in directives) {
      val value = directive.getValueElement()
      val text = value?.unwrappedText

      val allowValues = when (directive.name) {
        VISIBILITY_DIRECTIVE -> setOf(VISIBILITY_PRIVATE, VISIBILITY_REEXPORT)
        RESOLUTION_DIRECTIVE -> setOf(RESOLUTION_MANDATORY, RESOLUTION_OPTIONAL)
        else -> {
          holder.newAnnotation(
            HighlightSeverity.ERROR, ManifestBundle.message(
              "manifest.lang.specifyOnly", "$VISIBILITY_DIRECTIVE/$RESOLUTION_DIRECTIVE", "Directives"
            )
          ).range(directive.textRange).create()
          return true
        }
      }

      if (text.isNullOrBlank() || text !in allowValues) {
        holder.newAnnotation(
          HighlightSeverity.ERROR,
          ManifestBundle.message("manifest.lang.shouldBe", directive.name, allowValues.joinToString("/"))
        ).range(value?.highlightingRange ?: directive.textRange).create()
        return true
      }
    }

    return false
  }
}