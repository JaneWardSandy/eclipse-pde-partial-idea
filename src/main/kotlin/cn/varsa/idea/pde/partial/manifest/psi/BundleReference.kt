package cn.varsa.idea.pde.partial.manifest.psi

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.extension.parseVersionRange
import cn.varsa.idea.pde.partial.common.version.VersionRange
import cn.varsa.idea.pde.partial.core.manifest.BundleManifestIndex
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.lang.manifest.psi.HeaderValuePart
import org.jetbrains.lang.manifest.psi.ManifestFile

class BundleReference(element: HeaderValuePart) : PsiReferenceBase<HeaderValuePart>(element),
  EmptyResolveMessageProvider {

  override fun resolve(): PsiElement? =
    ResolveCache.getInstance(element.project).resolveWithCaching(this, RESOLVER, false, false)

  override fun getUnresolvedMessagePattern(): String = ManifestBundle.message("manifest.psi.cannotResolveBundle", canonicalText)

  companion object {
    private val RESOLVER = ResolveCache.Resolver { ref, _ ->
      val text = ref.canonicalText
      val refElement = ref.element
      val project = refElement.project

      if (text.isBlank() || !refElement.isValid) return@Resolver null

      val clause = refElement.parent as? ManifestHeaderPart.Clause ?: return@Resolver null
      val versionRange = clause.getAttributes()
        .firstOrNull { it.name == Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE }
        ?.getValue()
        .let {
          try {
            it.parseVersionRange()
          } catch (e: Exception) {
            VersionRange.ANY_VERSION_RANGE
          }
        }

      val refBSN = text.replace("\\s".toRegex(), "")
      val fileIndex = ProjectFileIndex.getInstance(project)
      val manifest = BundleManifestIndex.getAllManifestBySymbolicNames(setOf(refBSN), project)

      var file = manifest.filterKeys { fileIndex.getModuleForFile(it) != null }
        .filterValues { it.bundleVersion in versionRange }
        .maxByOrNull { it.value.bundleVersion }?.key

      if (file == null) {
        file = manifest.filterValues { it.bundleVersion in versionRange }.maxByOrNull { it.value.bundleVersion }?.key
      }

      file?.let { PsiManager.getInstance(project).findFile(it) }
        ?.let { it as? ManifestFile? }
        ?.let { it.getHeader(Constants.OSGI.Header.BUNDLE_SYMBOLICNAME) ?: it }
    }
  }
}