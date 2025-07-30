package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*
import org.jetbrains.lang.manifest.psi.*

object BundleClasspathParser : BundleManifestHeaderParser() {

  override fun checkClauses(
    header: Header,
    clauses: List<ManifestHeaderPart.Clause>,
    holder: AnnotationHolder,
  ): Boolean = if (clauses.none { it.getValue()?.unwrappedText == "." }) {
    holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.classpathMustConDot"))
      .range(header.textRange).create()
    true
  } else false

  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false

    if (value.unwrappedText.isBlank()) {
      holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(value.highlightingRange).create()
      return true
    }

    return false
  }
}