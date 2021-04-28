package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import com.intellij.codeInsight.daemon.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import org.jetbrains.kotlin.idea.util.*

class PluginsImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false

    override fun isImplicitUsage(element: PsiElement): Boolean = (element as? PsiClass)?.let {
        it.module?.run {
            getPluginsXmlContent(this)?.contains("\"${it.qualifiedName}\"") == true || BundleManifestCacheService.getInstance(
                project
            ).getManifest(this)?.bundleActivator == it.qualifiedName
        }
    } == true

    // FIXME: 2021/3/14 Plugins.xml
    private val caches = hashMapOf<String, CachedValue<String?>>()
    private fun getPluginsXmlContent(module: Module): String? =
        ModuleRootManager.getInstance(module).contentRoots.mapNotNull { it.findFileByRelativePath(PluginsXml) }
            .mapNotNull { PsiManager.getInstance(module.project).findFile(it) }.firstOrNull()?.let { file ->
                caches.computeIfAbsent(file.virtualFile.presentableUrl) {
                    CachedValuesManager.getManager(module.project).createCachedValue {
                        CachedValueProvider.Result.create(
                            if (file.project.isDisposed) "" else file.text, file.virtualFile, file
                        )
                    }
                }.value
            }
}
