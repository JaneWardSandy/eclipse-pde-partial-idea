package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.extension.parseVersion
import cn.varsa.idea.pde.partial.manifest.lang.BundleManifestHeaderParser
import cn.varsa.idea.pde.partial.manifest.psi.ManifestHeaderPart
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*

object BundleVersionParser : BundleManifestHeaderParser() {

  override fun allowMultiClauses(): Boolean = false
  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false
    val versionRangeText = value.unwrappedText

    if (versionRangeText.isBlank()) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(value.highlightingRange)
        .create()
      return true
    }

    try {
      versionRangeText.parseVersion()
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