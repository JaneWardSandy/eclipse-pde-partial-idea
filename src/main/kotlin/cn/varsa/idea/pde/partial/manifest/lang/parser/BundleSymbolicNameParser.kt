package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.SINGLETON_DIRECTIVE
import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*

object BundleSymbolicNameParser : BundleManifestHeaderParser() {

  override fun allowMultiClauses(): Boolean = false
  override fun checkDirectives(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val directives = clause.getDirectives()
    if (directives.isEmpty()) return false

    val directive = directives[0]
    if (directives.size > 1 || directive.name != SINGLETON_DIRECTIVE) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.specifyOnly", SINGLETON_DIRECTIVE, "Directives")
      ).range(directive.textRange).create()
      return true
    }

    val value = directive.getValueElement()
    if (value == null || value.unwrappedText !in setOf(true.toString(), false.toString())) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.shouldBe", SINGLETON_DIRECTIVE, "${true}/${false}")
      ).range(value?.highlightingRange ?: directive.textRange).create()
      return true
    }

    return false
  }
}