package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants.Eclipse.ECLIPSE_EXTENSIBLE_API
import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*

object EclipseExtensibleAPIParser : BundleManifestHeaderParser() {

  override fun allowMultiClauses(): Boolean = false
  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false

    if (value.unwrappedText.toBooleanStrictOrNull() == null) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.shouldBe", ECLIPSE_EXTENSIBLE_API, "${true}/${false}")
      ).range(value.highlightingRange).create()
      return true
    }

    return false
  }
}