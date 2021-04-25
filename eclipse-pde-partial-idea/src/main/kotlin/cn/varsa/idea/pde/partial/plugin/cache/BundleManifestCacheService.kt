package cn.varsa.idea.pde.partial.plugin.cache

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import com.intellij.openapi.components.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import org.jetbrains.lang.manifest.psi.*
import java.util.jar.*

class BundleManifestCacheService(private val project: Project) {
    private val caches = HashMap<String, CachedValue<BundleManifest?>>()

    // FIXME: 2021/4/22
//    val libSymbol2Versions = HashMap<String, HashSet<String>>()
//    val libSymbol2Manifest = HashMap<String, BundleManifest?>()
//    val libSymbol2ReExportSymbol = HashMap<String, HashSet<String>>()

    companion object {
        fun getInstance(project: Project): BundleManifestCacheService =
            ServiceManager.getService(project, BundleManifestCacheService::class.java)
    }

    fun clearCache() {
        caches.clear()
//        libSymbol2Versions.clear()
//        libSymbol2Manifest.clear()
//        libSymbol2ReExportSymbol.clear()
    }

    fun getManifest(psiClass: PsiClass): BundleManifest? = psiClass.containingFile?.let { getManifest(it) }

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

    fun getManifest(module: Module): BundleManifest? =
        ModuleRootManager.getInstance(module).contentRoots.mapNotNull { it.findFileByRelativePath(ManifestPath) }
            .mapNotNull { PsiManager.getInstance(module.project).findFile(it) }.mapNotNull { it as? ManifestFile }
            .firstOrNull()?.let { getManifest0(it) }

    fun getManifest(root: VirtualFile): BundleManifest? =
        if (root.extension?.toLowerCase() == "jar" && root.fileSystem != JarFileSystem.getInstance()) {
            JarFileSystem.getInstance().getJarRootForLocalFile(root)
        } else {
            root
        }?.findFileByRelativePath(ManifestPath)?.let { getManifest0(it) }

    private fun getManifest0(manifestFile: ManifestFile): BundleManifest? =
        caches.computeIfAbsent(manifestFile.virtualFile.presentableUrl) {
            CachedValuesManager.getManager(project).createCachedValue {
                CachedValueProvider.Result.create(readManifest(manifestFile), manifestFile.virtualFile, manifestFile)
            }
        }.value

    private fun getManifest0(virtualFile: VirtualFile): BundleManifest? =
        caches.computeIfAbsent(virtualFile.presentableUrl) {
            CachedValuesManager.getManager(project).createCachedValue {
                CachedValueProvider.Result.create(readManifest(virtualFile), virtualFile)
            }
        }.value

    private fun readManifest(manifestFile: ManifestFile): BundleManifest =
        manifestFile.text.byteInputStream().use(::Manifest).let(BundleManifest::parse)

    private fun readManifest(virtualFile: VirtualFile): BundleManifest =
        virtualFile.inputStream.use(::Manifest).let(BundleManifest::parse)
}
