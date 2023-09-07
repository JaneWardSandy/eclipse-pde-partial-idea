package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.SINGLETON_DIRECTIVE
import cn.varsa.idea.pde.partial.manifest.lang.BundleManifestHeaderParser
import cn.varsa.idea.pde.partial.manifest.psi.AssignmentExpression
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import org.jetbrains.lang.manifest.psi.*

object BundleSymbolicNameParser : BundleManifestHeaderParser() {

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

      return checkDirectives(clause.getDirectives(), valuePart, holder)
    }

    return false
  }

  private fun checkDirectives(
    directives: List<AssignmentExpression.Directive>,
    valuePart: HeaderValuePart,
    holder: AnnotationHolder,
  ): Boolean {
    if (directives.isEmpty()) return false
    val directive = directives[0]

    if (directives.size > 1 || directive.name != SINGLETON_DIRECTIVE) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.specifyOnly", SINGLETON_DIRECTIVE, "Directives")
      ).range(directives.firstOrNull()?.textRange ?: valuePart.highlightingRange).create()
      return true
    }

    val valueElement = directive.getValueElement()
    if (valueElement == null || valueElement.unwrappedText !in setOf(true.toString(), false.toString())) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.shouldBe", SINGLETON_DIRECTIVE, "${true}/${false}")
      ).range(valueElement?.highlightingRange ?: directive.textRange).create()
      return true
    }

    return false
  }
}