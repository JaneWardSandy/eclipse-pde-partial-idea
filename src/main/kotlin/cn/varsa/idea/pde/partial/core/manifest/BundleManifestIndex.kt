package cn.varsa.idea.pde.partial.core.manifest

import cn.varsa.idea.pde.partial.common.manifest.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.search.*
import com.intellij.util.*
import com.intellij.util.containers.*
import com.intellij.util.indexing.*
import com.jetbrains.rd.util.*
import org.jetbrains.annotations.*

object BundleManifestIndex {

  @ApiStatus.Internal val NAME: ID<String, BundleManifest> = ID.create("BundleManifestIndex")

  fun requireReIndexes() = FileBasedIndex.getInstance().requestRebuild(NAME)

  fun getManifestByFile(project: Project, file: VirtualFile): Map.Entry<String, BundleManifest>? =
    FileBasedIndex.getInstance().getFileData(NAME, file, project).firstOrNull()

  fun getAllManifest(project: Project): Map<VirtualFile, BundleManifest> {
    val manifests = CollectionFactory.createSmallMemoryFootprintMap<VirtualFile, BundleManifest>()
    processAllManifests(GlobalSearchScope.allScope(project)) { file, manifest ->
      manifests[file] = manifest
      true
    }
    return manifests
  }

  fun getManifestBySymbolicName(name: String, project: Project) = getAllManifestBySymbolicNames(setOf(name), project)

  fun getAllManifestBySymbolicNames(names: Collection<String>, project: Project): Map<VirtualFile, BundleManifest> {
    val manifests = CollectionFactory.createSmallMemoryFootprintMap<VirtualFile, BundleManifest>()
    processAllManifestsBySymbolicNames(names, GlobalSearchScope.allScope(project)) { file, manifest ->
      manifests[file] = manifest
      true
    }
    return manifests
  }

  fun processAllManifests(
    scope: GlobalSearchScope,
    filter: IdFilter? = null,
    processor: FileBasedIndex.ValueProcessor<BundleManifest>,
  ) = processAllSymbolicName(scope, filter) {
    FileBasedIndex.getInstance().processValues(NAME, it, null, processor, scope, filter)
    true
  }

  fun processAllSymbolicName(scope: GlobalSearchScope, filter: IdFilter? = null, processor: Processor<String>) {
    FileBasedIndex.getInstance().processAllKeys(NAME, processor, scope, filter)
  }

  fun processAllManifestsBySymbolicNames(
    bundleSymbolicNames: Collection<String>,
    scope: GlobalSearchScope,
    filter: IdFilter? = null,
    processor: FileBasedIndex.ValueProcessor<BundleManifest>,
  ) = bundleSymbolicNames.forEach {
    FileBasedIndex.getInstance().processValues(NAME, it, null, processor, scope, filter)
  }
}