package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.manifest.lang.BundleManifestHeaderParser
import cn.varsa.idea.pde.partial.manifest.psi.ManifestHeaderPart
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import com.intellij.openapi.projectRoots.JavaSdkVersion

object RequiredExecutionEnvironmentParser : BundleManifestHeaderParser() {
  val javaExecutionEnvironments = JavaSdkVersion.values().map {
    val feature = it.ordinal
    when {
      feature < 2 -> "JRE-1.$feature"
      feature < 6 -> "J2SE-1.$feature"
      feature < 9 -> "JavaSE-1.$feature"
      else -> "JavaSE-$feature"
    }
  }

  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false

    if (value.unwrappedText !in javaExecutionEnvironments) {
      holder
        .newAnnotation(HighlightSeverity.WARNING, ManifestBundle.message("manifest.lang.invalidJavaVersion"))
        .range(value.highlightingRange)
        .create()
      return true
    }

    return false
  }
}