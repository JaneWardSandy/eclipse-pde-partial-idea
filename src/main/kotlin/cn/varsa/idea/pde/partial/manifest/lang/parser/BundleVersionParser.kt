package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.extension.parseVersion
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import org.jetbrains.lang.manifest.header.impl.StandardHeaderParser
import org.jetbrains.lang.manifest.psi.*

object BundleVersionParser : StandardHeaderParser() {

  override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
    val valuePart = header.headerValue as? HeaderValuePart? ?: return false
    val versionRangeText = valuePart.unwrappedText

    if (versionRangeText.isBlank()) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(valuePart.highlightingRange)
        .create()
      return true
    }
    try {
      versionRangeText.parseVersion()
    } catch (e: Exception) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, e.message ?: e.localizedMessage)
        .range(valuePart.highlightingRange)
        .create()
      return true
    }

    return false
  }
}