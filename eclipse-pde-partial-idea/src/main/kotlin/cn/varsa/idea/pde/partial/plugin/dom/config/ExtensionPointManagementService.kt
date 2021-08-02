package cn.varsa.idea.pde.partial.plugin.dom.config

import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.dom.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.domain.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.jetbrains.rd.util.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class ExtensionPointManagementService(private val project: Project) : BackgroundResolvable {
    companion object {
        fun getInstance(project: Project): ExtensionPointManagementService =
            project.getService(ExtensionPointManagementService::class.java)
    }

    private val pointCacheService by lazy { ExtensionPointCacheService.getInstance(project) }
    private val xmlCacheService by lazy { PluginXmlCacheService.getInstance(project) }

    private val applications = hashSetOf<String>()
    private val products = hashSetOf<String>()
    private val epPoint2ExsdPath = ConcurrentHashMap<String, VirtualFile>()
    private val epReferenceIdentityMap =
        ConcurrentHashMap<Pair<String, String>, ConcurrentHashMap<String, HashSet<String>>>()

    override fun resolve(project: Project, indicator: ProgressIndicator) {
        val managementService = BundleManagementService.getInstance(project)

        ExtensionPointCacheService.getInstance(project).clearCache()
        val cacheService = PluginXmlCacheService.getInstance(project)
        cacheService.clearCache()

        indicator.checkCanceled()
        indicator.text = "Resolving extension point management"
        indicator.isIndeterminate = false
        indicator.fraction = 0.0

        val bundles = managementService.bundles.values
        val bundleStep = 1 / (bundles.size + 1)
        bundles.forEach { bundle ->
            indicator.checkCanceled()
            indicator.text2 = "Resolving bundle ${bundle.file}"

            cacheService.getXmlInfo(bundle)?.also { info ->
                applications += info.applications
                products += info.products
                epPoint2ExsdPath += info.epPoint2ExsdPath
                info.epReferenceIdentityMap.forEach { (key, attributes) ->
                    epReferenceIdentityMap.computeIfAbsent(key) { ConcurrentHashMap() }.also {
                        attributes.forEach { (name, values) -> it.computeIfAbsent(name) { hashSetOf() } += values }
                    }
                }
            }

            indicator.fraction += bundleStep
        }

        indicator.fraction = 1.0
    }

    fun getExtensionPoint(pointID: String): ExtensionPointDefinition? =
        (epPoint2ExsdPath[pointID] ?: project.allPDEModules().firstNotNullResult {
            xmlCacheService.getXmlInfo(it)?.epPoint2ExsdPath?.get(pointID)
        })?.let { pointCacheService.getExtensionPoint(it) }

    fun getExtensionPoints(): Set<String> =
        epPoint2ExsdPath.keys + project.allPDEModules().mapNotNull { xmlCacheService.getXmlInfo(it) }
            .flatMap { it.epPoint2ExsdPath.keys }

    fun getApplications(): Set<String> =
        applications + project.allPDEModules().mapNotNull { xmlCacheService.getXmlInfo(it) }.flatMap { it.applications }

    fun getProducts(): Set<String> =
        products + project.allPDEModules().mapNotNull { xmlCacheService.getXmlInfo(it) }.flatMap { it.products }

    fun isUsageByAnyExtension(value: String?): Boolean {
        epReferenceIdentityMap.values.any { name2values -> name2values.any { it.value.contains(value) } }
            .ifTrue { return true }

        project.allPDEModules().forEach { module ->
            xmlCacheService.getXmlInfo(module)?.epReferenceIdentityMap?.values?.any { name2values ->
                name2values.any { it.value.contains(value) }
            }?.ifTrue { return true }
        }

        return false
    }

    fun getReferenceIdentifies(point: String, extension: String, attribute: String): Set<String> =
        (epReferenceIdentityMap[point to extension]?.get(attribute) ?: emptySet()) + project.allPDEModules()
            .asSequence().mapNotNull { xmlCacheService.getXmlInfo(it) }.map { it.epReferenceIdentityMap }
            .mapNotNull { it[point to extension] }.mapNotNull { it[attribute] }.flatten()
}
