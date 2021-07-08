package cn.varsa.idea.pde.partial.plugin.dom.cache

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.dom.indexes.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.util.*
import com.jetbrains.rd.util.*
import org.jetbrains.kotlin.idea.util.*
import java.io.*
import javax.xml.stream.*

class PluginXmlCacheService(private val project: Project) {
    private val cacheService by lazy { BundleManifestCacheService.getInstance(project) }
    private val cachedValuesManager by lazy { CachedValuesManager.getManager(project) }

    private val caches = ConcurrentHashMap<String, CachedValue<XmlInfo?>>()
    private val lastIndexed = ConcurrentHashMap<String, XmlInfo>()

    companion object {
        fun getInstance(project: Project): PluginXmlCacheService = project.getService(PluginXmlCacheService::class.java)

        fun resolvePluginXml(file: VirtualFile, stream: InputStream = file.inputStream): XmlInfo? {
            val applications = hashSetOf<String>()
            val products = hashSetOf<String>()
            val epPoint2ExsdPath = hashMapOf<String, VirtualFile>()
            val epReferenceIdentityMap = hashMapOf<Pair<String, String>, HashMap<String, HashSet<String>>>()

            return try {
                resolvePluginXml(file, stream, applications, products, epPoint2ExsdPath, epReferenceIdentityMap)

                XmlInfo(applications, products, epPoint2ExsdPath, epReferenceIdentityMap)
            } catch (e: Exception) {
                thisLogger().warn("$PluginsXml file not valid: $file : $e")
                null
            }
        }

        private fun resolvePluginXml(
            file: VirtualFile,
            stream: InputStream,
            applications: HashSet<String>,
            products: HashSet<String>,
            epPoint2ExsdPath: HashMap<String, VirtualFile>,
            epReferenceIdentityMap: HashMap<Pair<String, String>, HashMap<String, HashSet<String>>>
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

                                    file.parent.findFileByRelativePath(schema)?.also { epPoint2ExsdPath[id] = it }
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
                                        epReferenceIdentityMap.computeIfAbsent(extensionPoint to reader.localName) { hashMapOf() }
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

    fun getXmlInfo(bundle: BundleDefinition): XmlInfo? =
        bundle.root.findChild(PluginsXml)?.let { getXmlInfo(bundle.bundleSymbolicName, it) }

    fun getXmlInfo(module: Module): XmlInfo? =
        module.moduleRootManager.contentRoots.mapNotNull { it.findChild(PluginsXml) }.firstOrNull()?.let {
            getXmlInfo(cacheService.getManifest(module)?.bundleSymbolicName?.key ?: module.name, it)
        }

    private fun getXmlInfo(bundleSymbolicName: String, file: VirtualFile): XmlInfo? =
        DumbService.isDumb(project).ifFalse { PluginXmlIndex.readXmlInfo(project, file) }
            ?.updateIdNames(bundleSymbolicName)?.also { lastIndexed[file.presentableUrl] = it }
            ?: lastIndexed[file.presentableUrl] ?: caches.computeIfAbsent(file.presentableUrl) {
                cachedValuesManager.createCachedValue {
                    CachedValueProvider.Result.create(
                        resolvePluginXml(file)?.updateIdNames(bundleSymbolicName), file
                    )
                }
            }.value

    private fun XmlInfo.updateIdNames(bundleSymbolicName: String): XmlInfo =
        XmlInfo(applications.map { if (it.startsWith(bundleSymbolicName)) it else "$bundleSymbolicName.$it" }
                    .toHashSet(),
                products.map { if (it.startsWith(bundleSymbolicName)) it else "$bundleSymbolicName.$it" }.toHashSet(),
                epPoint2ExsdPath.mapKeys { if (it.key.startsWith(bundleSymbolicName)) it.key else "$bundleSymbolicName.${it.key}" }
                    .toMap(hashMapOf()),
                epReferenceIdentityMap)
}
