package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import org.jetbrains.lang.manifest.header.impl.StandardHeaderParser
import org.jetbrains.lang.manifest.psi.*

object NotBlankValueParser : StandardHeaderParser() {

  override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
    val valuePart = header.headerValue as? HeaderValuePart ?: return false

    if (valuePart.unwrappedText.isBlank()) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(valuePart.highlightingRange)
        .create()
      return true
    }

    return false
  }
}