package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.manifest.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.util.*
import com.intellij.util.indexing.*

class BundleManifestCacheService(private val project: Project) {

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
  else FileBasedIndex.getInstance().getSingleEntryIndexData(BundleManifestIndex.KEY, manifestFile, project)

  private fun getManifestFiles(
    symbolName: String,
    processor: Processor<VirtualFile>,
    filter: GlobalSearchScope = GlobalSearchScope.projectScope(project),
  ): Boolean = if (DumbService.isDumb(project)) false
  else FileBasedIndex.getInstance().getFilesWithKey(BundleManifestNamesIndex.KEY, setOf(symbolName), processor, filter)
}
