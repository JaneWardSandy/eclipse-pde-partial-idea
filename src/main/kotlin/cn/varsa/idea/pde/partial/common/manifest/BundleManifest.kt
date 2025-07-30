package cn.varsa.idea.pde.partial.common.manifest

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.version.*
import com.jetbrains.rd.util.*
import java.util.concurrent.locks.*
import java.util.jar.*
import kotlin.concurrent.*

data class BundleManifest(val attribute: Map<String, String>) {
  constructor(manifest: Manifest) : this(manifest.mainAttributes.entries.associate { it.key.toString() to it.value.toString() })

  private val lock = ReentrantReadWriteLock()
  private val parameterCache = HashMap<String, ManifestParameters?>(attribute.size)

  fun getParameters(header: String): ManifestParameters? = lock.read { parameterCache[header] } ?: lock.write {
    parameterCache.getOrPut(header) { attribute[header]?.let { ManifestParameters.parse(it) } }
  }

  /** @see Constants.OSGI.Header.REQUIRE_BUNDLE */
  val requireBundle: ManifestParameters?
    get() = getParameters(Constants.OSGI.Header.REQUIRE_BUNDLE)

  /** @see Constants.OSGI.Header.IMPORT_PACKAGE */
  val importPackage: ManifestParameters?
    get() = getParameters(Constants.OSGI.Header.IMPORT_PACKAGE)

  /** @see Constants.OSGI.Header.EXPORT_PACKAGE */
  val exportPackage: ManifestParameters?
    get() = getParameters(Constants.OSGI.Header.EXPORT_PACKAGE)

  /** @see Constants.OSGI.Header.BUNDLE_CLASSPATH */
  val bundleClassPath: ManifestParameters?
    get() = getParameters(Constants.OSGI.Header.BUNDLE_CLASSPATH)

  /** @see Constants.OSGI.Header.BUNDLE_ACTIVATOR */
  val bundleActivator: String?
    get() = attribute[Constants.OSGI.Header.BUNDLE_ACTIVATOR]

  /** @see Constants.OSGI.Header.BUNDLE_REQUIREDEXECUTIONENVIRONMENT */
  val bundleRequiredExecutionEnvironment: ManifestParameters?
    get() = getParameters(Constants.OSGI.Header.BUNDLE_REQUIREDEXECUTIONENVIRONMENT)

  /** @see Constants.OSGI.Header.BUNDLE_SYMBOLICNAME */
  val bundleSymbolicName: Map.Entry<String, ParameterAttributes>?
    get() = getParameters(Constants.OSGI.Header.BUNDLE_SYMBOLICNAME)?.attributes?.firstOrNull()

  /** @see Constants.OSGI.Header.FRAGMENT_HOST */
  val fragmentHost: Map.Entry<String, ParameterAttributes>?
    get() = getParameters(Constants.OSGI.Header.FRAGMENT_HOST)?.attributes?.firstOrNull()

  /** @see Constants.OSGI.Header.BUNDLE_VERSION */
  val bundleVersion: Version = attribute[Constants.OSGI.Header.BUNDLE_VERSION].parseVersion()

  /** @see Constants.Eclipse.ECLIPSE_SOURCE_BUNDLE */
  val eclipseSourceBundle: Map.Entry<String, ParameterAttributes>?
    get() = getParameters(Constants.Eclipse.ECLIPSE_SOURCE_BUNDLE)?.attributes?.firstOrNull()

  /** @see Constants.Eclipse.ECLIPSE_EXTENSIBLE_API */
  val eclipseExtensibleAPI: Boolean
    get() = attribute[Constants.Eclipse.ECLIPSE_EXTENSIBLE_API].toBoolean()

  override fun toString(): String = attribute.entries.joinToString(
    System.lineSeparator(), System.lineSeparator()
  ) { (header, value) -> "$header: $value" }
}
