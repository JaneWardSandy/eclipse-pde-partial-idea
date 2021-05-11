package cn.varsa.idea.pde.partial.plugin.manifest.psi

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import com.intellij.codeInsight.daemon.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.*
import org.jetbrains.lang.manifest.psi.*
import org.osgi.framework.Constants.*

class BundleReference(element: HeaderValuePart) : PsiReferenceBase<HeaderValuePart>(element),
                                                  EmptyResolveMessageProvider {
    override fun resolve(): PsiElement? =
        ResolveCache.getInstance(element.project).resolveWithCaching(this, resolver, false, false)

    override fun getUnresolvedMessagePattern(): String = message("manifest.lang.cannotResolveBundle", canonicalText)

    companion object {
        private val resolver = ResolveCache.AbstractResolver<BundleReference, PsiElement> { ref, _ ->
            val text = ref.canonicalText
            val refElement = ref.element

            if (text.isNotBlank() && refElement.isValid) {
                ModuleUtilCore.findModuleForPsiElement(refElement)?.let { module ->
                    val result = Ref.create<PsiElement>()
                    val refText = text.replace("\\s".toRegex(), "")

                    val cacheService = BundleManifestCacheService.getInstance(module.project)

                    ModuleRootManager.getInstance(module).run {
                        orderEntries().forEachModule { orderModule ->
                            cacheService.getManifest(orderModule)?.bundleSymbolicName?.key?.takeIf { it == refText }
                                ?.let { orderModule.rootManager.contentRoots }
                                ?.mapNotNull { root -> root.findFileByRelativePath(ManifestPath) }?.firstOrNull()
                                ?.let { PsiManager.getInstance(orderModule.project).findFile(it) }
                                ?.let { (it as? ManifestFile)?.getHeader(BUNDLE_SYMBOLICNAME) ?: it }
                                ?.also(result::set) == null
                        }

                        if (result.isNull) {
                            orderEntries().forEachLibrary { orderLib ->
                                orderLib.getFiles(OrderRootType.CLASSES)
                                    .firstOrNull { cacheService.getManifest(it)?.bundleSymbolicName?.key == refText }
                                    ?.findFileByRelativePath(ManifestPath)
                                    ?.let { PsiManager.getInstance(module.project).findFile(it) }
                                    ?.let { it as? ManifestFile }?.let { it.getHeader(BUNDLE_SYMBOLICNAME) ?: it }
                                    ?.also(result::set) == null
                            }
                        }
                    }
                    result.get()
                }
            } else {
                null
            }
        }
    }
}
