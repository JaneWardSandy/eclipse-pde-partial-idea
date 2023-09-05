package cn.varsa.idea.pde.partial.manifest.psi

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.extension.parseVersionRange
import cn.varsa.idea.pde.partial.common.version.VersionRange
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.lang.manifest.psi.HeaderValuePart

class BundleReference(element: HeaderValuePart) : PsiReferenceBase<HeaderValuePart>(element),
  EmptyResolveMessageProvider {

  override fun resolve(): PsiElement? =
    ResolveCache.getInstance(element.project).resolveWithCaching(this, RESOLVER, false, false)

  override fun getUnresolvedMessagePattern(): String = ManifestBundle.message("psi.cannotResolveBundle", canonicalText)

  companion object {
    private val RESOLVER = ResolveCache.Resolver { ref, _ ->
      val text = ref.canonicalText
      val refElement = ref.element

      if (text.isBlank() || !refElement.isValid) return@Resolver null
      val module = ModuleUtilCore.findModuleForPsiElement(refElement) ?: return@Resolver null

      val clause = refElement.parent as? AssignmentExpression.Clause ?: return@Resolver null
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
      val result = Ref.create<PsiElement>()

      // TODO

      result.get()
    }
  }
}