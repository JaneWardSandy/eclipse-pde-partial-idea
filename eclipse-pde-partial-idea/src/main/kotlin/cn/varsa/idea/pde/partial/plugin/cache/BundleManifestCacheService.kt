package cn.varsa.idea.pde.partial.plugin.cache

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.jetbrains.rd.util.*
import org.jetbrains.lang.manifest.psi.*
import java.util.jar.*
import kotlin.io.use

class BundleManifestCacheService(private val project: Project) {

    // Key was manifest file path
    // will maintain key's relation to the same value on CacheValue update
    private val caches = ConcurrentHashMap<String, CachedValue<BundleManifest>>()

    companion object {
        fun getInstance(project: Project): BundleManifestCacheService =
            project.getService(BundleManifestCacheService::class.java)
    }

    fun clearCache() {
        caches.clear()
    }

    fun getManifest(psiClass: PsiClass): BundleManifest? = psiClass.containingFile?.let(this::getManifest)

    fun getManifest(item: PsiFileSystemItem): BundleManifest? {
        val file = item.virtualFile
        if (file != null) {
            val index = ProjectFileIndex.getInstance(item.project)
            val list = index.getOrderEntriesForFile(file)
            if (list.size == 1 && list.first() is JdkOrderEntry) return null

            val module = index.getModuleForFile(file)
            if (module != null) return getManifest(module)

            val libRoot = index.getClassRootForFile(file)
            if (libRoot != null) return getManifest(libRoot)
        }

        return null
    }

    fun getManifest(module: Module): BundleManifest? = getManifestPsi(module)?.let(this::getManifest0)
    fun getManifest(root: VirtualFile): BundleManifest? = getManifestFile(root)?.let(this::getManifest0)

    private fun getManifestPsi(module: Module): ManifestFile? =
        ModuleRootManager.getInstance(module).contentRoots.mapNotNull { it.findFileByRelativePath(ManifestPath) }
            .mapNotNull { PsiManager.getInstance(module.project).findFile(it) }.mapNotNull { it as? ManifestFile }
            .firstOrNull()

    private fun getManifestFile(root: VirtualFile): VirtualFile? =
        if (root.extension?.toLowerCase() == "jar" && root.fileSystem != JarFileSystem.getInstance()) {
            JarFileSystem.getInstance().getJarRootForLocalFile(root)
        } else {
            root
        }?.findFileByRelativePath(ManifestPath)

    private fun getManifest0(manifestFile: ManifestFile): BundleManifest =
        getManifest0({ manifestFile.virtualFile.presentableUrl },
                     { readManifest(manifestFile) },
                     { arrayOf(manifestFile.virtualFile, manifestFile) })

    private fun getManifest0(manifestFile: VirtualFile): BundleManifest =
        getManifest0({ manifestFile.presentableUrl }, { readManifest(manifestFile) }, { arrayOf(manifestFile) })

    private fun getManifest0(
        keyProvider: () -> String, manifestProvider: () -> BundleManifest, dependenciesProvider: () -> Array<Any>
    ): BundleManifest = caches.computeIfAbsent(keyProvider()) {
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result.create(manifestProvider(), dependenciesProvider())
        }
    }.value

    private fun readManifest(manifestFile: ManifestFile): BundleManifest =
        manifestFile.text.byteInputStream().use(::Manifest).let(BundleManifest::parse)

    private fun readManifest(virtualFile: VirtualFile): BundleManifest =
        virtualFile.inputStream.use(::Manifest).let(BundleManifest::parse)
}
