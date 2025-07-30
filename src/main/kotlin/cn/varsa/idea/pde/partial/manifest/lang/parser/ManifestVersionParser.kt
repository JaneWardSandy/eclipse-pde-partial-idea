package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*
import org.jetbrains.lang.manifest.psi.*

object ManifestVersionParser : BundleManifestHeaderParser() {

  override fun getConvertedValue(header: Header): Any? = header.headerValue?.unwrappedText?.toIntOrNull()
  override fun allowMultiClauses(): Boolean = false
  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false

    if (value.unwrappedText.toIntOrNull() != 2) {
      holder.newAnnotation(HighlightSeverity.WARNING, ManifestBundle.message("manifest.lang.manifestVersion2"))
        .range(value.highlightingRange).create()
      return true
    }

    return false
  }
}