package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import org.jetbrains.lang.manifest.header.impl.*
import org.jetbrains.lang.manifest.psi.*

object BundleActivatorParser : ClassReferenceParser() {

  override fun checkClass(valuePart: HeaderValuePart, aClass: PsiClass, holder: AnnotationHolder): Boolean {
    val bundleActivatorClazz = JavaPsiFacade.getInstance(valuePart.project)
      .findClass("org.osgi.framework.BundleActivator", ProjectScope.getLibrariesScope(valuePart.project))

    if (bundleActivatorClazz != null && !aClass.isInheritor(bundleActivatorClazz, true)) {
      holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidActivator"))
        .range(valuePart.highlightingRange).create()
      return true
    }

    return false
  }
}