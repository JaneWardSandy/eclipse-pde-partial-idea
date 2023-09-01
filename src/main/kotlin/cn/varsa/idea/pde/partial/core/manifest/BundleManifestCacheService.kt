package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.manifest.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.util.indexing.*

// fixme 2023/02/04: Is it still necessary to exist?
@Service(Service.Level.PROJECT)
class BundleManifestCacheService(private val project: Project) {

  companion object {
    @JvmStatic fun getInstance(project: Project): BundleManifestCacheService =
      project.getService(BundleManifestCacheService::class.java)
  }

  fun getBundleManifest(psiClass: PsiClass): BundleManifest? = getBundleManifest(psiClass.containingFile)

  fun getBundleManifest(item: PsiFileSystemItem): BundleManifest? {
    val virtualFile = item.virtualFile ?: return null
    val fileIndex = ProjectFileIndex.getInstance(project)

    val isBelongJDK = fileIndex.getOrderEntriesForFile(virtualFile).any { it is JdkOrderEntry }
    if (isBelongJDK) return null

    val module = fileIndex.getModuleForFile(virtualFile)
    if (module != null) return getBundleManifest(module)

    val libRoot = fileIndex.getClassRootForFile(virtualFile)
    if (libRoot != null) return getBundleManifest(libRoot)

    return null
  }

  fun getBundleManifest(module: Module): BundleManifest? {
    val rootManager = ModuleRootManager.getInstance(module)

    val manifestFile =
      rootManager.contentRoots.firstNotNullOfOrNull { it.findFileByRelativePath(Constants.Partial.File.MANIFEST_PATH) }
        ?: return null

    return getManifestByFile(manifestFile)
  }

  fun getBundleManifest(root: VirtualFile): BundleManifest? {
    if (!root.isValid) return null

    val isJarFile = root.extension?.equals("jar", true) ?: false
    val isJarFileSystem = root.fileSystem is JarFileSystem

    val jarFile = if (isJarFile && !isJarFileSystem) {
      JarFileSystem.getInstance().getJarRootForLocalFile(root) ?: return null
    } else root

    val manifestFile = jarFile.findFileByRelativePath(Constants.Partial.File.MANIFEST_PATH) ?: return null

    return getManifestByFile(manifestFile)
  }

  private fun getManifestByFile(manifestFile: VirtualFile): BundleManifest? = if (DumbService.isDumb(project)) null
  else FileBasedIndex.getInstance().getSingleEntryIndexData(BundleManifestIndex.id, manifestFile, project)
}
