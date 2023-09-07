package cn.varsa.idea.pde.partial.manifest.lang

import cn.varsa.idea.pde.partial.common.Constants
import cn.varsa.idea.pde.partial.common.Constants.Eclipse.ECLIPSE_EXTENSIBLE_API
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_ACTIVATIONPOLICY
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_ACTIVATOR
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_CLASSPATH
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_NAME
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_SYMBOLICNAME
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VENDOR
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.EXPORT_PACKAGE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_HOST
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.IMPORT_PACKAGE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.REQUIRE_BUNDLE
import cn.varsa.idea.pde.partial.manifest.lang.parser.*
import org.jetbrains.lang.manifest.header.*

class BasicBundleManifestHeaderParserProvider : HeaderParserProvider {

  override fun getHeaderParsers(): Map<String, HeaderParser> = mapOf(
    BUNDLE_VERSION to ManifestVersionParser,
    BUNDLE_NAME to NotBlankValueParser,
    BUNDLE_VENDOR to NotBlankValueParser,
    BUNDLE_SYMBOLICNAME to BundleSymbolicNameParser,
    BUNDLE_VERSION to BundleVersionParser,
    BUNDLE_ACTIVATOR to BundleActivatorParser,
    BUNDLE_ACTIVATIONPOLICY to BundleActivationPolicyParser,
    REQUIRE_BUNDLE to RequireBundleParser,
//    IMPORT_PACKAGE
//    EXPORT_PACKAGE
//    FRAGMENT_HOST
//    BUNDLE_CLASSPATH

    ECLIPSE_EXTENSIBLE_API to EclipseExtensibleAPIParser,
  )
}