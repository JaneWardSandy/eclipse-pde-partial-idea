package cn.varsa.idea.pde.partial.plugin.dom.cache

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.dom.indexes.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.util.*
import com.jetbrains.rd.util.*
import java.io.*
import javax.xml.stream.*

@Service(Service.Level.PROJECT)
class PluginXmlCacheService(private val project: Project) {
  private val cacheService by lazy { BundleManifestCacheService.getInstance(project) }
  private val cachedValuesManager by lazy { CachedValuesManager.getManager(project) }

  private val caches = ConcurrentHashMap<String, CachedValue<XmlInfo?>>()
  private val lastIndexed = ConcurrentHashMap<String, XmlInfo>()

  companion object {
    fun getInstance(project: Project): PluginXmlCacheService = project.getService(PluginXmlCacheService::class.java)

    fun resolvePluginXml(
      bundleRoot: VirtualFile,
      bundleSourceRoot: VirtualFile?,
      pluginXmlFile: VirtualFile,
      stream: InputStream = pluginXmlFile.inputStream
    ): XmlInfo? {
      val applications = hashSetOf<String>()
      val products = hashSetOf<String>()
      val epPoint2ExsdPath = hashMapOf<String, VirtualFile>()
      val epReferenceIdentityMap = ConcurrentHashMap<Pair<String, String>, ConcurrentHashMap<String, HashSet<String>>>()

      return try {
        resolvePluginXml(
          bundleRoot, bundleSourceRoot, stream, applications, products, epPoint2ExsdPath, epReferenceIdentityMap
        )

        XmlInfo(applications, products, epPoint2ExsdPath, epReferenceIdentityMap)
      } catch (e: Exception) {
        thisLogger().warn("${pluginXmlFile.name} file not valid: $pluginXmlFile : $e", e)
        null
      }
    }

    private fun resolvePluginXml(
      bundleRoot: VirtualFile,
      bundleSourceRoot: VirtualFile?,
      stream: InputStream,
      applications: HashSet<String>,
      products: HashSet<String>,
      epPoint2ExsdPath: HashMap<String, VirtualFile>,
      epReferenceIdentityMap: ConcurrentHashMap<Pair<String, String>, ConcurrentHashMap<String, HashSet<String>>>
    ) {
      val reader = XMLInputFactory.newInstance().createXMLStreamReader(stream)
      try {
        var extensionPoint = ""

        loop@ while (reader.hasNext()) {
          when (reader.next()) {
            XMLStreamConstants.START_ELEMENT -> {
              when (reader.localName) {
                "extension-point" -> {
                  val id = reader.getAttributeValue("", "id") ?: continue@loop
                  val schema = reader.getAttributeValue("", "schema") ?: continue@loop

                  (bundleRoot.findFileByRelativePath(schema)
                    ?: bundleSourceRoot?.findFileByRelativePath(schema))?.also {
                    epPoint2ExsdPath[id] = it
                  }
                }

                "extension" -> {
                  extensionPoint = reader.getAttributeValue("", "point") ?: continue@loop
                  val id = reader.getAttributeValue("", "id") ?: continue@loop

                  if (extensionPoint == "org.eclipse.core.runtime.applications") {
                    applications += id
                  } else if (extensionPoint == "org.eclipse.core.runtime.products") {
                    products += id
                  }
                }

                else -> if (extensionPoint.isNotBlank()) {
                  val map =
                    epReferenceIdentityMap.computeIfAbsent(extensionPoint to reader.localName) { ConcurrentHashMap() }
                  (0 until reader.attributeCount).map { index ->
                    reader.getAttributeLocalName(index) to reader.getAttributeValue(index)
                  }.filterNot { it.first.isBlank() || it.second.isBlank() }.forEach { (name, value) ->
                    map.computeIfAbsent(name) { hashSetOf() } += value
                  }
                }
              }
            }

            XMLStreamConstants.END_ELEMENT -> {
              when (reader.localName) {
                "extension" -> extensionPoint = ""
              }
            }
          }
        }
      } finally {
        reader.close()
      }
    }
  }

  fun clearCache() {
    caches.clear()
    lastIndexed.clear()
  }

  fun getXmlInfo(bundle: BundleDefinition): XmlInfo? = bundle.root.validFileOrRequestResolve()?.let(::findXml)
    ?.let { getXmlInfo(bundle.bundleSymbolicName, it, bundle.root, bundle.sourceBundle?.root) }

  fun getXmlInfo(module: Module): XmlInfo? =
    module.moduleRootManager.contentRoots.firstNotNullOfOrNull(::findXml)?.let {
      getXmlInfo(cacheService.getManifest(module)?.bundleSymbolicName?.key ?: module.name, it, it.parent)
    }

  private fun findXml(root: VirtualFile?): VirtualFile? =
    root?.children?.firstOrNull { it.name == PluginsXml || it.name == FragmentXml }

  private fun getXmlInfo(
    bundleSymbolicName: String,
    pluginXmlFile: VirtualFile,
    bundleRoot: VirtualFile,
    bundleSourceRoot: VirtualFile? = null
  ): XmlInfo? = readCompute {
    pluginXmlFile.validFileOrRequestResolve()?.let { file ->
      DumbService.isDumb(project).runFalse { PluginXmlIndex.readXmlInfo(project, file) }
        ?.updateIdNames(bundleSymbolicName)?.also { lastIndexed[file.presentableUrl] = it }
        ?: lastIndexed[file.presentableUrl] ?: caches.computeIfAbsent(file.presentableUrl) {
          cachedValuesManager.createCachedValue {
            CachedValueProvider.Result.create(
              resolvePluginXml(bundleRoot, bundleSourceRoot, file)?.updateIdNames(
                bundleSymbolicName
              ), file
            )
          }
        }.value
    }
  }

  private fun XmlInfo.updateIdNames(bundleSymbolicName: String): XmlInfo =
    XmlInfo(applications.map { if (it.startsWith(bundleSymbolicName) || it.contains('.')) it else "$bundleSymbolicName.$it" }
              .toHashSet(),
            products.map { if (it.startsWith(bundleSymbolicName) || it.contains('.')) it else "$bundleSymbolicName.$it" }
              .toHashSet(),
            epPoint2ExsdPath.mapKeys { if (it.key.startsWith(bundleSymbolicName) || it.key.contains('.')) it.key else "$bundleSymbolicName.${it.key}" }
              .toMap(hashMapOf()),
            epReferenceIdentityMap)

  private fun VirtualFile.validFileOrRequestResolve() =
    validFileOrRequestResolve(project) { "${it.presentableUrl} file not valid when build plugin.xml cache, maybe it was delete after load, please check, restart application or re-resolve workspace" }
}
