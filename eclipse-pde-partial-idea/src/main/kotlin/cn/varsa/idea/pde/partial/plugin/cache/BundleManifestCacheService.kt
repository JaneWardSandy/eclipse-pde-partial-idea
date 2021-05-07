package cn.varsa.idea.pde.partial.plugin.cache

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.components.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import org.jetbrains.lang.manifest.psi.*
import org.osgi.framework.*
import java.util.jar.*

class BundleManifestCacheService(private val project: Project) {

    // Key was manifest file path, or bundle symbol name
    // will maintain key's relation to the same value on CacheValue update
    private val caches = HashMap<String, CachedValue<BundleManifest>>()

    // Auto update by field #caches's value update
    private val versionCache = HashMap<String, Version?>()

    // Auto update by field #caches's value update
    private val manifestPath2BundleSymbolName = HashMap<String, String?>()

    // Key was bundle symbol name
    // TODO: 2021/4/30 cycle dependency detector, cache update?
    private val libReExportRequiredSymbolName = HashMap<String, HashSet<String>>()

    companion object {
        fun getInstance(project: Project): BundleManifestCacheService =
            ServiceManager.getService(project, BundleManifestCacheService::class.java)
    }

    fun clearCache() {
        caches.clear()
        versionCache.clear()
        manifestPath2BundleSymbolName.clear()
        libReExportRequiredSymbolName.clear()
    }

    fun buildCache() {
        val libPair = LibraryTablesRegistrar.getInstance().getLibraryTable(project).run {
            DependencyScope.values().map { it.displayName }
                .mapNotNull { getLibraryByName("$ProjectLibraryNamePrefix$it") }
                .mapNotNull { it.getFiles(OrderRootType.CLASSES) }
        }.flatMap { it.toList() }.mapNotNull(this::getManifest).associate { manifest ->
            manifest.bundleSymbolicName?.key to manifest.reExportRequiredBundleSymbolNames
        }

        libPair.filterNot { it.key == null }
            .forEach { libReExportRequiredSymbolName.computeIfAbsent(it.key!!) { HashSet() } += it.value }

        libReExportRequiredSymbolName.forEach { (symbolName, reExport) ->
            fillDependencies(symbolName, reExport, reExport, libPair)
        }
    }

    private tailrec fun fillDependencies(
        symbolName: String, reExport: HashSet<String>, next: Set<String>, libPair: Map<String?, Set<String>?>
    ) {
        val nextSet = next.filterNot { it == symbolName }.mapNotNull { libPair[it] }.flatten().toSet()
        if (reExport.addAll(nextSet)) fillDependencies(symbolName, reExport, nextSet, libPair)
    }

    fun getManifestByBundleSymbolName(bundleSymbolName: String): BundleManifest? = caches[bundleSymbolName]?.value
    fun getVersionByBundleSymbolName(bundleSymbolName: String): Version? = versionCache[bundleSymbolName]
    fun getReExportRequiredBundleBySymbolName(bundleSymbolName: String): Set<String> =
        libReExportRequiredSymbolName[bundleSymbolName] ?: emptySet()

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
            val manifest = manifestProvider()

            val newSymbolName = manifest.bundleSymbolicName?.key
            val oldSymbolName = manifestPath2BundleSymbolName[it]
            if (newSymbolName != oldSymbolName) {
                manifestPath2BundleSymbolName[it] = newSymbolName

                val oldCache = caches.remove(oldSymbolName)
                if (newSymbolName != null && oldCache != null) caches[newSymbolName] = oldCache
            }
            if (newSymbolName != null) versionCache[newSymbolName] = manifest.bundleVersion

            CachedValueProvider.Result.create(manifest, dependenciesProvider())
        }
    }.also { it.value.bundleSymbolicName?.key?.also { symbolName -> caches[symbolName] = it } }.value

    private fun readManifest(manifestFile: ManifestFile): BundleManifest =
        manifestFile.text.byteInputStream().use(::Manifest).let(BundleManifest::parse)

    private fun readManifest(virtualFile: VirtualFile): BundleManifest =
        virtualFile.inputStream.use(::Manifest).let(BundleManifest::parse)
}
