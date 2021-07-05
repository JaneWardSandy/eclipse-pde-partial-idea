package cn.varsa.idea.pde.partial.plugin.dom.cache

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.domain.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.util.*
import com.jetbrains.rd.util.*
import javax.xml.stream.*

class PluginXmlCacheService(private val project: Project) {
    private val cacheService by lazy { BundleManifestCacheService.getInstance(project) }
    private val cachedValuesManager by lazy { CachedValuesManager.getManager(project) }

    private val caches = ConcurrentHashMap<String, CachedValue<XmlInfo?>>()

    companion object {
        fun getInstance(project: Project): PluginXmlCacheService = project.getService(PluginXmlCacheService::class.java)
    }

    fun clearCache() {
        caches.clear()
    }

    fun getXmlInfo(bundle: BundleDefinition): XmlInfo? =
        bundle.root.findChild(PluginsXml)?.let { getXmlInfo(bundle.bundleSymbolicName, it) }

    fun getXmlInfo(module: Module): XmlInfo? =
        module.moduleRootManager.contentRoots.mapNotNull { it.findChild(PluginsXml) }.firstOrNull()?.let {
            getXmlInfo(cacheService.getManifest(module)?.bundleSymbolicName?.key ?: module.name, it)
        }

    private fun getXmlInfo(bundleSymbolicName: String, file: VirtualFile): XmlInfo? =
        caches.computeIfAbsent(file.presentableUrl) {
            cachedValuesManager.createCachedValue {
                val applications = hashSetOf<String>()
                val products = hashSetOf<String>()
                val epPoint2ExsdPath = hashMapOf<String, VirtualFile>()
                val epReferenceIdentityMap = hashMapOf<Pair<String, String>, HashMap<String, HashSet<String>>>()

                resolvePluginXml(
                    bundleSymbolicName, file, applications, products, epPoint2ExsdPath, epReferenceIdentityMap
                )

                // FIXME: 2021/6/29 PSI change, file update too late
                CachedValueProvider.Result.create(
                    XmlInfo(applications, products, epPoint2ExsdPath, epReferenceIdentityMap), file
                )
            }
        }.value

    private fun resolvePluginXml(
        bundleSymbolicName: String,
        file: VirtualFile,
        applications: HashSet<String>,
        products: HashSet<String>,
        epPoint2ExsdPath: HashMap<String, VirtualFile>,
        epReferenceIdentityMap: HashMap<Pair<String, String>, HashMap<String, HashSet<String>>>
    ) {
        val reader = XMLInputFactory.newInstance().createXMLStreamReader(file.inputStream)
        try {
            var extensionPoint = ""

            loop@ while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT -> {
                        when (reader.localName) {
                            "extension-point" -> {
                                val id = reader.getAttributeValue("", "id") ?: continue@loop
                                val schema = reader.getAttributeValue("", "schema") ?: continue@loop

                                val point = if (id.startsWith(bundleSymbolicName)) id else "$bundleSymbolicName.$id"
                                file.parent.findFileByRelativePath(schema)?.also { epPoint2ExsdPath[point] = it }
                            }
                            "extension" -> {
                                extensionPoint = reader.getAttributeValue("", "point") ?: continue@loop
                                val id = reader.getAttributeValue("", "id") ?: continue@loop

                                if (extensionPoint == "org.eclipse.core.runtime.applications") {
                                    applications += if (id.startsWith(bundleSymbolicName)) id else "$bundleSymbolicName.$id"
                                } else if (extensionPoint == "org.eclipse.core.runtime.products") {
                                    products += if (id.startsWith(bundleSymbolicName)) id else "$bundleSymbolicName.$id"
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
