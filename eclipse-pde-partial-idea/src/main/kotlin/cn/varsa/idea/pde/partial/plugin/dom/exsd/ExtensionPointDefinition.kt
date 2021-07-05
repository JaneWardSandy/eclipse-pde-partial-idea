package cn.varsa.idea.pde.partial.plugin.dom.exsd

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.dom.*
import cn.varsa.idea.pde.partial.plugin.dom.cache.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.util.*
import org.jdom.*
import org.jdom.filter2.*
import org.jdom.input.*
import org.jdom.xpath.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class ExtensionPointDefinition(exsd: VirtualFile) {
    companion object {
        const val schemaProtocol = "schema://"

        private val schemaInfoPath = XPathFactory.instance().compile(
            "/schema/annotation/appinfo/meta.schema | /schema/annotation/appInfo/meta.schema", Filters.element()
        )
        private val includePath = XPathFactory.instance().compile("/schema/include", Filters.element())
        private val extensionPath =
            XPathFactory.instance().compile("/schema/element[@name='extension']", Filters.element())
        private val elementPath =
            XPathFactory.instance().compile("/schema/element[@name!='extension']", Filters.element())
    }

    val plugin: String
    val id: String
    val name: String

    val point: String get() = id.takeIf { it.startsWith(plugin) } ?: "$plugin.$id"
    val includes: List<String>
    val extension: ElementDefinition?
    val elements: List<ElementDefinition>

    init {
        val document = SAXBuilder().apply { saxHandlerFactory = NameSpaceCleanerFactory() }.build(exsd.inputStream)

        schemaInfoPath.evaluateFirst(document).also {
            plugin = it.getAttributeValue("plugin")
            id = it.getAttributeValue("id")
            name = it.getAttributeValue("name")
        }
        includes = includePath.evaluate(document).mapNotNull { it.getAttributeValue("schemaLocation") }
        extension = extensionPath.evaluateFirst(document)?.let(::ElementDefinition)
        elements = elementPath.evaluate(document).map(::ElementDefinition)
    }

    fun findRefElement(
        ref: ElementRefDefinition, project: Project
    ): ElementDefinition? =
        findRefElement(this, ref, project, ExtensionPointCacheService.getInstance(project), hashSetOf())

    private fun findRefElement(
        definition: ExtensionPointDefinition,
        ref: ElementRefDefinition,
        project: Project,
        cacheService: ExtensionPointCacheService,
        includeVisited: HashSet<ExtensionPointDefinition>
    ): ElementDefinition? {
        if (!includeVisited.add(definition)) return null
        return definition.elements.firstOrNull { it.name == ref.ref }
            ?: definition.includes.mapNotNull { schemaLocation ->
                cacheService.loadExtensionPoint(schemaLocation)
                    .also { if (it == null) thisLogger().warn("Schema not existed: $schemaLocation") } // TODO: 2021/6/29 Add notify balloon
            }.firstNotNullResult { findRefElement(it, ref, project, cacheService, includeVisited) }
    }
}

class ElementDefinition(element: Element) {
    companion object {
        private val elementDeprecatedPath = XPathFactory.instance()
            .compile("annotation/appinfo/meta.element | annotation/appInfo/meta.element", Filters.element())
        private val elementRefPath =
            XPathFactory.instance().compile("complexType/descendant::element", Filters.element())
        private val attributePath = XPathFactory.instance().compile("complexType/attribute", Filters.element())
    }

    val name: String
    val type: String?
    val deprecated: Boolean

    val elementRefs: List<ElementRefDefinition>
    val attributes: List<AttributeDefinition>

    init {
        name = element.getAttributeValue("name")
        type = element.getAttributeValue("type")

        deprecated = elementDeprecatedPath.evaluateFirst(element)?.getAttributeBooleanValue("deprecated") ?: false
        elementRefs = elementRefPath.evaluate(element).map(::ElementRefDefinition)
        attributes = attributePath.evaluate(element).map(::AttributeDefinition)
    }
}

class ElementRefDefinition(element: Element) {
    val ref: String
    val minOccurs: Int
    val maxOccurs: Int

    init {
        ref = element.getAttributeValue("ref")
        minOccurs = element.getAttributeValue("minOccurs")?.toIntOrNull() ?: 1
        maxOccurs = element.getAttributeValue("maxOccurs")?.run { toIntOrNull() ?: -1 } ?: 1
    }
}

class AttributeDefinition(element: Element) {
    companion object {
        private val metaPath = XPathFactory.instance()
            .compile("annotation/appinfo/meta.attribute | annotation/appInfo/meta.attribute", Filters.element())
        private val simplePath = XPathFactory.instance().compile("simpleType/restriction", Filters.element())
        private val simpleEnumerationPath = XPathFactory.instance().compile("enumeration", Filters.element())
    }

    val name: String
    val type: String?
    val use: String?
    val value: String?

    val kind: String?
    val basedOn: String?
    val deprecated: Boolean

    val simpleBaseType: String?
    val simpleEnumeration: List<String>?

    init {
        name = element.getAttributeValue("name")
        type = element.getAttributeValue("type")
        use = element.getAttributeValue("use")
        value = element.getAttributeValue("value")

        metaPath.evaluateFirst(element).also {
            kind = it?.getAttributeValue("kind")
            basedOn = it?.getAttributeValue("basedOn")
            deprecated = it?.getAttributeBooleanValue("deprecated") ?: false
        }

        simplePath.evaluateFirst(element).also { simple ->
            simpleBaseType = simple?.getAttributeValue("base")
            simpleEnumeration = simple?.let(simpleEnumerationPath::evaluate)?.map { it.getAttributeValue("value") }
        }
    }
}
