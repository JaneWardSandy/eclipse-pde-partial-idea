package cn.varsa.idea.pde.partial.plugin.dom.cache

import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.dom.indexes.*
import cn.varsa.idea.pde.partial.plugin.openapi.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.util.*
import com.jetbrains.rd.util.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*

class ExtensionPointCacheService(private val project: Project) {
    private val cacheService by lazy { BundleManifestCacheService.getInstance(project) }
    private val bundleManagementService by lazy { BundleManagementService.getInstance(project) }
    private val cachedValuesManager by lazy { CachedValuesManager.getManager(project) }

    private val caches = ConcurrentHashMap<String, CachedValue<ExtensionPointDefinition?>>()
    private val lastIndexed = ConcurrentHashMap<String, ExtensionPointDefinition>()

    companion object {
        fun getInstance(project: Project): ExtensionPointCacheService =
            project.getService(ExtensionPointCacheService::class.java)

        fun resolveExtensionPoint(
            project: Project, schemaFile: VirtualFile, stream: InputStream = schemaFile.inputStream
        ): ExtensionPointDefinition? = try {
            ExtensionPointDefinition(stream)
        } catch (e: Exception) {
            PdeNotifier.getInstance(project).important("Schema invalid", "EXSD file not valid: $schemaFile : $e")
            null
        }
    }

    fun clearCache() {
        caches.clear()
        lastIndexed.clear()
    }

    fun loadExtensionPoint(schemaLocation: String): ExtensionPointDefinition? {
        val urlFragments = schemaLocation.substringAfter(ExtensionPointDefinition.schemaProtocol).split('/')

        val entry = urlFragments.subList(1, urlFragments.size).joinToString("/")
        return bundleManagementService.getBundlesByBSN(urlFragments[0])?.values?.firstNotNullOrNull { bundle ->
            loadExtensionPoint(bundle.root, entry) ?: bundle.sourceBundle?.let { loadExtensionPoint(it.root, entry) }
        } ?: project.allPDEModules()
            .firstOrNull { cacheService.getManifest(it)?.bundleSymbolicName?.key == urlFragments[0] }?.moduleRootManager?.contentRoots?.firstNotNullResult {
                loadExtensionPoint(it, entry)
            }
    }

    private fun loadExtensionPoint(root: VirtualFile, schema: String): ExtensionPointDefinition? =
        root.findFileByRelativePath(schema)?.let(this::getExtensionPoint)

    fun getExtensionPoint(file: VirtualFile): ExtensionPointDefinition? = readCompute {
        DumbService.isDumb(project).runFalse { ExtensionPointIndex.readEPDefinition(project, file) }
            ?.also { lastIndexed[file.presentableUrl] = it } ?: lastIndexed[file.presentableUrl]
        ?: caches.computeIfAbsent(file.presentableUrl) {
            cachedValuesManager.createCachedValue {
                CachedValueProvider.Result.create(resolveExtensionPoint(project, file), file)
            }
        }.value
    }
}
