package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import org.jetbrains.lang.manifest.header.impl.StandardHeaderParser
import org.jetbrains.lang.manifest.psi.*

object EclipseExtensibleAPIParser : StandardHeaderParser() {

  override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
    val valuePart = header.headerValue as? HeaderValuePart? ?: return false

    if (valuePart.unwrappedText.toBooleanStrictOrNull() == null) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.shouldBe", Constants.Eclipse.ECLIPSE_EXTENSIBLE_API, "${true}/${false}")
      ).range(valuePart.highlightingRange).create()
      return true
    }

    return false
  }
}