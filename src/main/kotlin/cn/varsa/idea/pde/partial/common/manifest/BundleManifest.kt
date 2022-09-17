package cn.varsa.idea.pde.partial.common.manifest

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.version.*
import com.jetbrains.rd.util.*
import java.util.concurrent.locks.*
import java.util.jar.*
import kotlin.concurrent.*

class BundleManifest(private val attribute: Map<String, String>) {
  constructor(manifest: Manifest) : this(manifest.mainAttributes.entries.associate { it.key.toString() to it.value.toString() })

  private val lock = ReentrantReadWriteLock()
  private val parameterCache = HashMap<String, ManifestParameters?>(attribute.size)

  fun getParameters(header: String): ManifestParameters? = lock.read { parameterCache[header] } ?: lock.write {
    parameterCache.getOrPut(header) { attribute[header]?.let(::ManifestParameters) }
  }

  fun getRequireBundle() = getParameters(Constants.OSGI.Header.REQUIRE_BUNDLE)
  fun getImportPackage() = getParameters(Constants.OSGI.Header.IMPORT_PACKAGE)
  fun getExportPackage() = getParameters(Constants.OSGI.Header.EXPORT_PACKAGE)
  fun getBundleClassPath() = getParameters(Constants.OSGI.Header.BUNDLE_CLASSPATH)
  fun getBundleActivator() = attribute[Constants.OSGI.Header.BUNDLE_ACTIVATOR]
  fun getBundleRequiredExecutionEnvironment() = getParameters(Constants.OSGI.Header.BUNDLE_REQUIREDEXECUTIONENVIRONMENT)
  fun getBundleSymbolicName() = getParameters(Constants.OSGI.Header.BUNDLE_SYMBOLICNAME)?.attributes?.firstOrNull()
  fun getFragmentHost() = getParameters(Constants.OSGI.Header.FRAGMENT_HOST)?.attributes?.firstOrNull()
  val bundleVersion: Version = attribute[Constants.OSGI.Header.BUNDLE_VERSION].parseVersion()
  fun getRequireCapability() = getParameters(Constants.OSGI.Header.REQUIRE_CAPABILITY)
  fun getProvideCapability() = getParameters(Constants.OSGI.Header.PROVIDE_CAPABILITY)
  fun getBundleName() = attribute[Constants.OSGI.Header.BUNDLE_NAME]
  fun getBundleDescription() = attribute[Constants.OSGI.Header.BUNDLE_DESCRIPTION]
  fun getBundleCopyright() = attribute[Constants.OSGI.Header.BUNDLE_COPYRIGHT]
  fun getBundleDocUrl() = attribute[Constants.OSGI.Header.BUNDLE_DOCURL]
  fun getBundleVendor() = attribute[Constants.OSGI.Header.BUNDLE_VENDOR]
  fun getBundleContactAddress() = attribute[Constants.OSGI.Header.BUNDLE_CONTACTADDRESS]
  fun getBundleCategory() = attribute[Constants.OSGI.Header.BUNDLE_CATEGORY]
  fun getBundleNativeCode() = attribute[Constants.OSGI.Header.BUNDLE_NATIVECODE]

  fun getEclipseSourceBundle() = getParameters(Constants.OSGI.Eclipse.ECLIPSE_SOURCE_BUNDLE)?.attributes?.firstOrNull()
  fun getEclipseExtensibleAPI() = attribute[Constants.OSGI.Eclipse.ECLIPSE_EXTENSIBLE_API].toBoolean()

  override fun toString(): String = "BundleManifest($attribute)"
  override fun hashCode(): Int = attribute.hashCode()
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BundleManifest) return false
    if (attribute != other.attribute) return false
    return true
  }
}
