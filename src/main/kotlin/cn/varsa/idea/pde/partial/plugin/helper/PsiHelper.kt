package cn.varsa.idea.pde.partial.plugin.helper

import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.text.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.util.*
import com.intellij.util.*
import org.jetbrains.lang.manifest.*
import org.jetbrains.lang.manifest.psi.*

object PsiHelper {
  fun getActivatorClass(project: Project): PsiClass? = CachedValuesManager.getManager(project).getCachedValue(project) {
    val psiClass = JavaPsiFacade.getInstance(project)
      .findClass("org.osgi.framework.BundleActivator", ProjectScope.getLibrariesScope(project))
    CachedValueProvider.Result.create(psiClass, PsiModificationTracker.MODIFICATION_COUNT)
  }

  fun isActivator(element: PsiElement): Boolean =
    (element as? PsiClass)?.takeUnless { it.hasModifierProperty(PsiModifier.ABSTRACT) }
      ?.let { getActivatorClass(it.project)?.let { activator -> it.isInheritor(activator, true) } } == true

  fun resolvePackage(element: PsiElement, packageName: String): Array<PsiDirectory> =
    JavaPsiFacade.getInstance(element.project).findPackage(packageName)?.getDirectories(
      ModuleUtilCore.findModuleForPsiElement(element)?.getModuleWithDependenciesAndLibrariesScope(false)
        ?: ProjectScope.getAllScope(element.project)
    ) ?: PsiDirectory.EMPTY_ARRAY

  fun isHeader(element: PsiElement?, headerName: String): Boolean = element is Header && headerName == element.name

  fun setHeader(manifestFile: ManifestFile, headerName: String, headerValue: String) {
    val header = manifestFile.getHeader(headerName)
    val newHeader = createHeader(manifestFile.project, headerName, headerValue)
    if (header != null) {
      header.replace(newHeader)
    } else {
      addHeader(manifestFile, newHeader)
    }
  }

  fun appendToHeader(manifestFile: ManifestFile, headerName: String, headerValue: String) {
    var valueText = headerValue
    val header = manifestFile.getHeader(headerName)
    if (header != null) {
      val oldValue = header.headerValue
      if (oldValue != null) {
        var oldText = StringUtil.trimTrailing(header.text.substring(oldValue.startOffsetInParent, header.textLength))
        if (oldText.isNotEmpty()) oldText += ",\n "
        valueText = oldText + valueText
      }
      header.replace(createHeader(manifestFile.project, headerName, valueText))
    } else {
      addHeader(manifestFile, createHeader(manifestFile.project, headerName, valueText))
    }
  }

  private fun createHeader(project: Project, headerName: String, valueText: String): Header {
    val text = "$headerName: $valueText\n"
    val file = PsiFileFactory.getInstance(project).createFileFromText("DUMMY.MF", ManifestFileType.INSTANCE, text)
    return (file as ManifestFile).getHeader(headerName) ?: throw IncorrectOperationException("Bad header: '$text'")
  }

  private fun addHeader(manifestFile: ManifestFile, newHeader: Header) {
    val section = manifestFile.mainSection
    val headers = manifestFile.headers
    when {
      section == null -> manifestFile.add(newHeader.parent)
      headers.isEmpty() -> section.addBefore(newHeader, section.firstChild)
      else -> section.addAfter(newHeader, headers.last())
    }
  }
}
