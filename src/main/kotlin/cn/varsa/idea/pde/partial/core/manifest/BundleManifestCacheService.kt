package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.search.*
import com.intellij.util.*
import com.intellij.util.indexing.*

class BundleManifestCacheService(private val project: Project) {

  private fun getManifestByFile(manifestFile: VirtualFile): BundleManifest? =
    FileBasedIndex.getInstance().getSingleEntryIndexData(BundleManifestIndex.KEY, manifestFile, project)

  private fun getManifestFiles(
    symbolName: String,
    processor: Processor<VirtualFile>,
    filter: GlobalSearchScope = GlobalSearchScope.projectScope(project),
  ) = FileBasedIndex.getInstance().getFilesWithKey(BundleManifestNamesIndex.KEY, setOf(symbolName), processor, filter)
}
