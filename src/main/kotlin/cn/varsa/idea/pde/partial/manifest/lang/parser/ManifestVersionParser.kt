package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import org.jetbrains.lang.manifest.header.impl.StandardHeaderParser
import org.jetbrains.lang.manifest.psi.*

object ManifestVersionParser : StandardHeaderParser() {

  override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
    val valuePart = header.headerValue as? HeaderValuePart ?: return false

    if (valuePart.unwrappedText.toIntOrNull() != 2) {
      holder
        .newAnnotation(HighlightSeverity.WARNING, ManifestBundle.message("manifest.lang.manifestVersion2"))
        .range(valuePart.highlightingRange)
        .create()
      return true
    }

    return false
  }

  override fun getConvertedValue(header: Header): Any? = header.headerValue?.unwrappedText?.toIntOrNull()
}