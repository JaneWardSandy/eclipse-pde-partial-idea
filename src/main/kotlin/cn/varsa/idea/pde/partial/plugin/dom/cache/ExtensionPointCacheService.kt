package cn.varsa.idea.pde.partial.plugin.dom.cache

import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.dom.indexes.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.util.*
import com.jetbrains.rd.util.*
import java.io.*

@Service(Service.Level.PROJECT)
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
      schemaFile: VirtualFile, stream: InputStream = schemaFile.inputStream
    ): ExtensionPointDefinition? = try {
      ExtensionPointDefinition(schemaFile, stream)
    } catch (e: Exception) {
      thisLogger().warn("EXSD file not valid: $schemaFile : $e", e)
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
    return bundleManagementService.getBundlesByBSN(urlFragments[0])?.values?.firstNotNullOfOrNull { bundle ->
      loadExtensionPoint(bundle.root, entry) ?: bundle.sourceBundle?.let { loadExtensionPoint(it.root, entry) }
    } ?: project.allPDEModules()
      .firstOrNull { cacheService.getManifest(it)?.bundleSymbolicName?.key == urlFragments[0] }?.moduleRootManager?.contentRoots?.firstNotNullOfOrNull {
        loadExtensionPoint(it, entry)
      }
  }

  private fun loadExtensionPoint(root: VirtualFile, schema: String): ExtensionPointDefinition? =
    root.validFileOrRequestResolve()?.findFileByRelativePath(schema)?.let(this::getExtensionPoint)

  fun getExtensionPoint(schemaFile: VirtualFile): ExtensionPointDefinition? = readCompute {
    schemaFile.validFileOrRequestResolve()?.let { file ->
      DumbService.isDumb(project).runFalse { ExtensionPointIndex.readEPDefinition(project, file) }
        ?.also { lastIndexed[file.presentableUrl] = it } ?: lastIndexed[file.presentableUrl] ?: caches.computeIfAbsent(
        file.presentableUrl
      ) {
        cachedValuesManager.createCachedValue {
          CachedValueProvider.Result.create(resolveExtensionPoint(file), file)
        }
      }.value
    }
  }

  private fun VirtualFile.validFileOrRequestResolve() =
    validFileOrRequestResolve(project) { "${it.presentableUrl} file not valid when build extension point cache, maybe it was delete after load, please check, restart application or re-resolve workspace" }
}
