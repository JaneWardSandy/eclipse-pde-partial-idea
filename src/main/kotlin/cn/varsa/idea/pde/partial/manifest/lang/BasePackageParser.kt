package cn.varsa.idea.pde.partial.manifest.lang

import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.*
import com.intellij.psi.search.*
import org.jetbrains.lang.manifest.psi.*

abstract class BasePackageParser : BundleManifestHeaderParser() {

  override fun getConvertedValue(header: Header): Any? =
    header.headerValues.mapNotNull { it as? ManifestHeaderPart.Clause? }.mapNotNull { it.getValue() }
      .map { it.unwrappedText }

  override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> =
    if (headerValuePart.parent !is ManifestHeaderPart.Clause) PsiReference.EMPTY_ARRAY
    else getPackageReferences(headerValuePart)

  protected fun getPackageReferences(element: PsiElement): Array<PsiReference> {
    var packageName = element.text.replace("\\s".toRegex(), "")
    if (packageName.isBlank()) return PsiReference.EMPTY_ARRAY

    var offset = 0
    if (packageName.startsWith('!')) {
      packageName = packageName.removePrefix("!")
      offset = 1
    }
    if (packageName.endsWith('?')) {
      packageName = packageName.removeSuffix("?")
    }

    return PackageReferenceSet(packageName, element, offset).references.toTypedArray()
  }

  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false
    val packageName = value.unwrappedText.removeSuffix(".*")

    if (packageName.isBlank()) {
      holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
        .range(value.highlightingRange).create()
      return true
    }

    val empty = JavaPsiFacade.getInstance(clause.project).findPackage(packageName)
      ?.getDirectories(ProjectScope.getAllScope(clause.project)).isNullOrEmpty()
    if (empty) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.cannotResolvePackage", packageName)
      ).range(value.highlightingRange).create()
      return true
    }

    return false
  }
}