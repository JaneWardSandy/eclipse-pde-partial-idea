package cn.varsa.idea.pde.partial.common.domain

import cn.varsa.idea.pde.partial.common.support.*
import org.osgi.framework.*
import org.osgi.framework.Constants.*
import java.util.concurrent.*
import java.util.jar.*
import kotlin.collections.set

const val ECLIPSE_EXTENSIBLE_API = "Eclipse-ExtensibleAPI"
const val ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle"

class BundleManifest private constructor(private val map: HashMap<String, String>) : Map<String, String> by map {
  private val parametersMap = ConcurrentHashMap<String, Parameters?>()

  companion object {
    fun parse(map: Map<String, String>) = BundleManifest(map.toMap(hashMapOf()))
    fun parse(manifest: Manifest) =
      manifest.mainAttributes.entries.associate { it.key.toString() to it.value.toString() }.let { parse(it) }
  }

  fun getParameters(key: String): Parameters? = parametersMap.computeIfAbsent(key) { get(key)?.let(::Parameters) }

  val requireBundle by lazy { getParameters(REQUIRE_BUNDLE) }
  val importPackage by lazy { getParameters(IMPORT_PACKAGE) }
  val exportPackage by lazy { getParameters(EXPORT_PACKAGE) }
  val bundleClassPath by lazy { getParameters(BUNDLE_CLASSPATH) }
  val bundleActivator by lazy { get(BUNDLE_ACTIVATOR) }
  val bundleRequiredExecutionEnvironment by lazy { getParameters(BUNDLE_REQUIREDEXECUTIONENVIRONMENT) }
  val bundleSymbolicName by lazy { getParameters(BUNDLE_SYMBOLICNAME)?.entries?.firstOrNull() }
  val fragmentHost by lazy { getParameters(FRAGMENT_HOST)?.entries?.firstOrNull() }
  val bundleVersion: Version by lazy { get(BUNDLE_VERSION).parseVersion() }
  val requireCapability by lazy { getParameters(REQUIRE_CAPABILITY) }
  val provideCapability by lazy { getParameters(PROVIDE_CAPABILITY) }
  val bundleName by lazy { get(BUNDLE_NAME) }
  val bundleDescription by lazy { get(BUNDLE_DESCRIPTION) }
  val bundleCopyright by lazy { get(BUNDLE_COPYRIGHT) }
  val bundleDocUrl by lazy { get(BUNDLE_DOCURL) }
  val bundleVendor by lazy { get(BUNDLE_VENDOR) }
  val bundleContactAddress by lazy { get(BUNDLE_CONTACTADDRESS) }
  val bundleCategory by lazy { get(BUNDLE_CATEGORY) }
  val bundleNativeCode by lazy { get(BUNDLE_NATIVECODE) }

  val eclipseSourceBundle by lazy { getParameters(ECLIPSE_SOURCE_BUNDLE)?.entries?.firstOrNull() }
  val eclipseExtensibleAPI by lazy { get(ECLIPSE_EXTENSIBLE_API).toBoolean() }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BundleManifest

    if (map != other.map) return false

    return true
  }

  override fun hashCode(): Int {
    return map.hashCode()
  }

  override fun toString(): String = "BundleManifest(map=$map)"
}

class Parameters(value: String, private val map: MutableMap<String, Attrs> = mutableMapOf()) :
  Map<String, Attrs> by map {

  init {
    val qt = QuotedTokenizer(value, ";=,")
    var del: Char

    do {
      val attribute = mutableMapOf<String, String>()
      val directive = mutableMapOf<String, String>()
      val aliases = mutableListOf<String>()
      val name = qt.nextToken(",;")

      del = qt.separator
      if (name.isNullOrBlank()) {
        if (name == null) break
      } else {
        aliases += name

        while (del == ';') {
          val adName = qt.nextToken()
          if (qt.separator.also { del = it } != '=') {
            if (adName != null && adName.isNotBlank()) aliases += adName
          } else {
            val adValue = qt.nextToken() ?: ""
            del = qt.separator

            if (adName.isNullOrBlank()) continue
            if (adName.endsWith(':')) {
              directive[adName.dropLast(1)] = adValue
            } else {
              attribute[adName] = adValue
            }
          }
        }

        val attrs = Attrs(attribute, directive)
        aliases.forEach { map[it] = attrs }
      }
    } while (del == ',')
  }
}

class Attrs(val attribute: Map<String, String> = mutableMapOf(), val directive: Map<String, String> = mutableMapOf()) {
  private fun toStringAttribute(): String = attribute.takeIf { it.isNotEmpty() }?.entries?.joinToString(";", ";") ?: ""

  private fun toStringDirective(): String =
    directive.takeIf { it.isNotEmpty() }?.entries?.joinToString(";", ";") { "${it.key}:=${it.value}" } ?: ""

  override fun toString(): String = toStringAttribute() + toStringDirective()
}
