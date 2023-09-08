package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_HOST
import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.annotation.*
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.lang.manifest.psi.*

object FragmentHostParser : BundleManifestHeaderParser() {

  override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> =
    if (headerValuePart.parent is ManifestHeaderPart.Clause) arrayOf(BundleReference(headerValuePart)) else PsiReference.EMPTY_ARRAY

  override fun allowMultiClauses(): Boolean = false
  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val value = clause.getValue() ?: return false

    val fragmentHost = value.unwrappedText

    val bundleSymbolicName = PsiTreeUtil
      .getChildrenOfTypeAsList(PsiTreeUtil.getParentOfType(clause, Section::class.java), Header::class.java)
      .firstOrNull { it.name == Constants.OSGI.Header.BUNDLE_SYMBOLICNAME }?.headerValue?.let { it as? ManifestHeaderPart.Clause? }
    if (fragmentHost == bundleSymbolicName?.getValue()?.unwrappedText) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.hostCannotBeSelf"))
        .range(value.highlightingRange)
        .create()
      return true
    }

    return CommonManifestHeaderParser.checkManifestWithBundleVersionRange(fragmentHost, clause, holder) {
      ManifestBundle.message("manifest.lang.hostWasFragment")
    }
  }

  override fun checkAttributes(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val attributes = clause.getAttributes()
    if (attributes.isEmpty()) return false

    val attribute = attributes[0]
    if (attributes.size > 1 || attribute.name != BUNDLE_VERSION_ATTRIBUTE) {
      holder.newAnnotation(
        HighlightSeverity.ERROR,
        ManifestBundle.message("manifest.lang.specifyOnly", BUNDLE_VERSION_ATTRIBUTE, FRAGMENT_HOST)
      ).range(attribute.textRange ?: clause.textRange).create()
      return true
    }

    return CommonManifestHeaderParser.checkVersionRange(attribute, holder)
  }
}