package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.ACTIVATION_LAZY
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_ACTIVATIONPOLICY
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.EXCLUDE_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.INCLUDE_DIRECTIVE
import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*

object BundleActivationPolicyParser : BundleManifestHeaderParser() {

  override fun allowMultiClauses(): Boolean = false
  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false

    if (value.unwrappedText != ACTIVATION_LAZY) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.specifyOnly", ACTIVATION_LAZY, BUNDLE_ACTIVATIONPOLICY)
      ).range(value.highlightingRange).create()
      return true
    }

    return false
  }

  override fun checkDirectives(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val directives = clause.getDirectives()
    if (directives.isEmpty()) return false

    val directive = directives[0]
    if (directives.size > 1 || directive.name !in setOf(INCLUDE_DIRECTIVE, EXCLUDE_DIRECTIVE)) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.specifyOnly", "$INCLUDE_DIRECTIVE/$EXCLUDE_DIRECTIVE", "Directives")
      ).range(directive.textRange).create()
      return true
    }

    val value = directive.getValueElement()
    if (value == null || value.unwrappedText.isBlank()) {
      holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(value?.highlightingRange ?: directive.textRange).create()
      return true
    }

    return false
  }
}