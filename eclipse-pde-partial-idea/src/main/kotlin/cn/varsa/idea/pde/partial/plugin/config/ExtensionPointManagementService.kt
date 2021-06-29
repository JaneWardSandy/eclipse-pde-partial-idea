package cn.varsa.idea.pde.partial.plugin.config

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.exsd.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.sun.xml.fastinfoset.stax.factory.*
import javax.xml.stream.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.filterNot
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.hashMapOf
import kotlin.collections.hashSetOf
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.plusAssign
import kotlin.collections.set

class ExtensionPointManagementService : BackgroundResolvable {
    companion object {
        fun getInstance(project: Project): ExtensionPointManagementService =
            project.getService(ExtensionPointManagementService::class.java)
    }

    val applications = hashSetOf<String>()
    val products = hashSetOf<String>()
    val epPoint2ExsdPath = hashMapOf<String, VirtualFile>()
    val epReferenceIdentityMap = hashMapOf<Pair<String, String>, HashMap<String, HashSet<String>>>()

    override fun resolve(project: Project, indicator: ProgressIndicator) {
        val managementService = BundleManagementService.getInstance(project)
        val cacheService = BundleManifestCacheService.getInstance(project)

        ExtensionPointCacheService.getInstance(project).clearCache()

        indicator.checkCanceled()
        indicator.text = "Resolving extension point management"
        indicator.isIndeterminate = false
        indicator.fraction = 0.0

        val bundles = managementService.bundles.values
        val bundleStep = 0.9 / (bundles.size + 1)
        bundles.forEach { bundle ->
            indicator.checkCanceled()
            indicator.text2 = "Resolving bundle ${bundle.file}"

            val bundleSymbolicName = bundle.bundleSymbolicName

            bundle.root.findChild(PluginsXml)?.also { it.refresh(false, false) }
                ?.also { resolvePluginXml(bundleSymbolicName, it) }

            indicator.fraction += bundleStep
        }

        val pdeModules = project.allPDEModules()
        val moduleStep = 0.1 / (pdeModules.size + 1)
        pdeModules.forEach { module ->
            indicator.checkCanceled()
            indicator.text2 = "Resolving module ${module.name}"

            val bundleSymbolicName =
                readCompute { cacheService.getManifest(module) }?.bundleSymbolicName?.key ?: module.name

            module.moduleRootManager.contentRoots.mapNotNull { it.findChild(PluginsXml) }.firstOrNull()
                ?.also { it.refresh(false, false) }?.also { resolvePluginXml(bundleSymbolicName, it) }

            indicator.fraction += moduleStep
        }

        indicator.fraction = 1.0
    }

    fun getExtensionPoint(project: Project, pointID: String): ExtensionPointDefinition? =
        epPoint2ExsdPath[pointID]?.let { ExtensionPointCacheService.getInstance(project).getExtensionPoint(it) }

    private fun resolvePluginXml(bundleSymbolicName: String, file: VirtualFile) {
        val reader = StAXInputFactory.newInstance().createXMLStreamReader(file.inputStream)
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
