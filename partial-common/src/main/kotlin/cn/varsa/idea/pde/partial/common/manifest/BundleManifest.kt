package cn.varsa.idea.pde.partial.common.manifest

import cn.varsa.idea.pde.partial.common.constants.*
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.version.*
import java.util.concurrent.locks.*
import java.util.jar.*
import kotlin.concurrent.*

class BundleManifest(private val attribute: Map<String, String>) : Map<String, String> by attribute {
  constructor(manifest: Manifest) : this(manifest.mainAttributes.entries.associate { it.key.toString() to it.value.toString() })

  private val lock = ReentrantReadWriteLock()
  private val parameterCache = hashMapOf<String, ManifestParameters?>()

  fun getParameters(header: String): ManifestParameters? = lock.read { parameterCache[header] } ?: lock.write {
    parameterCache.getOrPut(header) { this[header]?.let(::ManifestParameters) }
  }

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

  override fun toString(): String = "BundleManifest($attribute)"
  override fun hashCode(): Int = attribute.hashCode()
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BundleManifest) return false
    if (attribute != other.attribute) return false
    return true
  }
}
