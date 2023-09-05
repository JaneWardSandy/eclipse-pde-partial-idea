package cn.varsa.idea.pde.partial.common

object Constants {
  object Partial {
    const val KILOBYTE = 1024
    const val MEGABYTE = KILOBYTE * KILOBYTE

    const val JAVA = "java"
    const val KOTLIN = "kotlin"

    object File {
      const val FEATURES = "features"
      const val DROPINS = "dropins"
      const val PLUGINS = "plugins"
      const val ARTIFACTS = "artifacts"

      const val MANIFEST_MF = "MANIFEST.MF"
      const val META_INF = "META-INF"
      const val MANIFEST_PATH = "$MANIFEST_MF/$META_INF"

      const val PLUGINS_XML = "plugin.xml"
      const val FRAGMENT_XML = "fragment.xml"
      const val FEATURE_XML = "feature.xml"
      const val BUILD_PROPERTIES = "build.properties"
    }

    object Module {
      const val PARTIAL_PREFIX = "Partial: "
      const val ARTIFACT_PREFIX = PARTIAL_PREFIX
      const val MODULE_LIBRARY_NAME = "Partial-Runtime"
      const val MODULE_COMPILE_ONLY_LIBRARY_NAME = "Partial-CompileOnly"
      const val PROJECT_LIBRARY_NAME_PREFIX = PARTIAL_PREFIX

      const val KOTLIN_BUNDLE_SYMBOL_NAME = "org.jetbrains.kotlin.osgi-bundle"
      const val KOTLIN_ORDER_ENTRY_NAME = "KotlinJavaRuntime"
    }
  }

  object Eclipse {
    const val ECLIPSE_EXTENSIBLE_API = "Eclipse-ExtensibleAPI"
    const val ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle"
  }

  object OSGI {
    const val SYSTEM_BUNDLE = "system.bundle"
    const val ORG_ECLIPSE_OSGI = "org.eclipse.osgi"

    object Header {
      /**
       * Manifest header identifying a list of directories and embedded JAR files,
       * which are bundle resources used to extend the bundle's classpath.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       */
      const val BUNDLE_CLASSPATH = "Bundle-ClassPath"

      /**
       * Manifest header identifying the bundle's name.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       */
      const val BUNDLE_NAME = "Bundle-Name"

      /**
       * Manifest header identifying the packages that the bundle offers to the
       * Framework for export.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       */
      const val EXPORT_PACKAGE = "Export-Package"

      /**
       * Manifest header identifying the packages on which the bundle depends.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       */
      const val IMPORT_PACKAGE = "Import-Package"

      /**
       * Manifest header identifying the bundle's vendor.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       */
      const val BUNDLE_VENDOR = "Bundle-Vendor"

      /**
       * Manifest header identifying the bundle's version.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       */
      const val BUNDLE_VERSION = "Bundle-Version"

      /**
       * Manifest header identifying the bundle's activator class.
       *
       * If present, this header specifies the name of the bundle resource class
       * that implements the `BundleActivator` interface and whose
       * `start` and `stop` methods are called by the Framework when
       * the bundle is started and stopped, respectively.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       */
      const val BUNDLE_ACTIVATOR = "Bundle-Activator"

      /**
       * Manifest header identifying the required execution environment for the
       * bundle. The service platform may run this bundle if any of the execution
       * environments named in this header matches one of the execution
       * environments it implements.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       *
       * @since 1.2
       * @deprecated As of 1.6. Replaced by the `osgi.ee` capability.
       */
      const val BUNDLE_REQUIREDEXECUTIONENVIRONMENT = "Bundle-RequiredExecutionEnvironment"

      /**
       * Manifest header identifying the bundle's symbolic name.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       *
       * @since 1.3
       */
      const val BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName"

      /**
       * Manifest header directive identifying whether a bundle is a singleton.
       * The default value is `false`.
       *
       * The directive value is encoded in the Bundle-SymbolicName manifest header
       * like:
       *
       *`
       * Bundle-SymbolicName: com.acme.module.test; singleton:=true
       * `
       *
       * @see BUNDLE_SYMBOLICNAME
       * @since 1.3
       */
      const val SINGLETON_DIRECTIVE = "singleton"

      /**
       * Manifest header directive identifying if and when a fragment may attach
       * to a host bundle. The default value is
       * [always][FRAGMENT_ATTACHMENT_ALWAYS].
       *
       * The directive value is encoded in the Bundle-SymbolicName manifest header
       * like:
       *
       *`
       * Bundle-SymbolicName: com.acme.module.test; fragment-attachment:="never"
       *`
       *
       * @see BUNDLE_SYMBOLICNAME
       * @see FRAGMENT_ATTACHMENT_ALWAYS
       * @see FRAGMENT_ATTACHMENT_RESOLVETIME
       * @see FRAGMENT_ATTACHMENT_NEVER
       * @since 1.3
       */
      const val FRAGMENT_ATTACHMENT_DIRECTIVE = "fragment-attachment"

      /**
       * Manifest header directive value identifying a fragment attachment type of
       * always. A fragment attachment type of always indicates that fragments are
       * allowed to attach to the host bundle at any time (while the host is
       * resolved or during the process of resolving the host bundle).
       *
       * The directive value is encoded in the Bundle-SymbolicName manifest header
       * like:
       *
       *`
       * Bundle-SymbolicName: com.acme.module.test; fragment-attachment:="always"
       *`
       *
       * @see FRAGMENT_ATTACHMENT_DIRECTIVE
       * @since 1.3
       */
      const val FRAGMENT_ATTACHMENT_ALWAYS = "always"

      /**
       * Manifest header directive value identifying a fragment attachment type of
       * resolve-time. A fragment attachment type of resolve-time indicates that
       * fragments are allowed to attach to the host bundle only during the
       * process of resolving the host bundle.
       *
       * The directive value is encoded in the Bundle-SymbolicName manifest header
       * like:
       *
       * `
       * Bundle-SymbolicName: com.acme.module.test;
       * fragment-attachment:="resolve-time"
       * `
       *
       * @see FRAGMENT_ATTACHMENT_DIRECTIVE
       * @since 1.3
       */
      const val FRAGMENT_ATTACHMENT_RESOLVETIME = "resolve-time"

      /**
       * Manifest header directive value identifying a fragment attachment type of
       * never. A fragment attachment type of never indicates that no fragments
       * are allowed to attach to the host bundle at any time.
       *
       * The directive value is encoded in the Bundle-SymbolicName manifest header
       * like:
       *
       *`
       * Bundle-SymbolicName: com.acme.module.test; fragment-attachment:="never"
       * `
       *
       * @see FRAGMENT_ATTACHMENT_DIRECTIVE
       * @since 1.3
       */
      const val FRAGMENT_ATTACHMENT_NEVER = "never"

      /**
       * Manifest header identifying the base name of the bundle's localization
       * entries.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       *
       * @see BUNDLE_LOCALIZATION_DEFAULT_BASENAME
       * @since 1.3
       */
      const val BUNDLE_LOCALIZATION = "Bundle-Localization"

      /**
       * Default value for the `Bundle-Localization` manifest header.
       *
       * @see BUNDLE_LOCALIZATION
       * @since 1.3
       */
      const val BUNDLE_LOCALIZATION_DEFAULT_BASENAME = "OSGI-INF/l10n/bundle"

      /**
       * Manifest header identifying the symbolic names of other bundles required
       * by the bundle.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       *
       * @since 1.3
       */
      const val REQUIRE_BUNDLE = "Require-Bundle"

      /**
       * Manifest header ATTRIBUTE identifying a range of versions for a bundle
       * specified in the `Require-Bundle` or `Fragment-Host` manifest
       * headers. The default value is `0.0.0`.
       *
       * The ATTRIBUTE value is encoded in the Require-Bundle manifest header
       * like:
       *
       *`
       * Require-Bundle: com.acme.module.test; bundle-version="1.1"
       *
       * Require-Bundle: com.acme.module.test; bundle-version="[1.0,2.0)"
       *`
       *
       * The bundle-version ATTRIBUTE value uses a mathematical interval notation
       * to specify a range of bundle versions. A bundle-version ATTRIBUTE value
       * specified as a single version means a version range that includes any
       * bundle version greater than or equal to the specified version.
       *
       * @see REQUIRE_BUNDLE
       * @since 1.3
       */
      const val BUNDLE_VERSION_ATTRIBUTE = "bundle-version"

      /**
       * Manifest header identifying the symbolic name of another bundle for which
       * that the bundle is a fragment.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       *
       * @since 1.3
       */
      const val FRAGMENT_HOST = "Fragment-Host"

      /**
       * Manifest header identifying the bundle manifest version. A bundle
       * manifest may express the version of the syntax in which it is written by
       * specifying a bundle manifest version. Bundles exploiting OSGi Release 4,
       * or later, syntax must specify a bundle manifest version.
       *
       * The bundle manifest version defined by OSGi Release 4 or, more
       * specifically, by version 1.3 of the OSGi Core Specification is "2".
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       *
       * @since 1.3
       */
      const val BUNDLE_MANIFESTVERSION = "Bundle-ManifestVersion"

      /**
       * Manifest header ATTRIBUTE identifying the version of a package specified
       * in the Export-Package or Import-Package manifest header.
       *
       * The ATTRIBUTE value is encoded in the Export-Package or Import-Package
       * manifest header like:
       *
       * `
       * Export-Package: org.osgi.framework; version="1.1"
       * `
       *
       * @see EXPORT_PACKAGE
       * @see IMPORT_PACKAGE
       * @since 1.3
       */
      const val VERSION_ATTRIBUTE = "version"

      /**
       * Manifest header ATTRIBUTE identifying the symbolic name of a bundle that
       * exports a package specified in the Import-Package manifest header.
       *
       * The ATTRIBUTE value is encoded in the Import-Package manifest header
       * like:
       *
       * `
       * Import-Package: org.osgi.framework; bundle-symbolic-name="com.acme.module.test"
       * `
       *
       * @see IMPORT_PACKAGE
       * @since 1.3
       */
      const val BUNDLE_SYMBOLICNAME_ATTRIBUTE = "bundle-symbolic-name"

      /**
       * Manifest header directive identifying the resolution type in the
       * Import-Package, Require-Bundle or Require-Capability manifest header. The
       * default value is [mandatory][RESOLUTION_MANDATORY].
       *
       * The directive value is encoded in the Import-Package, Require-Bundle or
       * Require-Capability manifest header like:
       *
       * `
       * Import-Package: org.osgi.framework; resolution:="optional"
       *
       * Require-Bundle: com.acme.module.test; resolution:="optional"
       *
       * Require-Capability: com.acme.capability; resolution:="optional"
       * `
       *
       * @see IMPORT_PACKAGE
       * @see REQUIRE_BUNDLE
       * @see REQUIRE_CAPABILITY
       * @see RESOLUTION_MANDATORY
       * @see RESOLUTION_OPTIONAL
       * @since 1.3
       */
      const val RESOLUTION_DIRECTIVE = "resolution"

      /**
       * Manifest header directive value identifying a mandatory resolution type.
       * A mandatory resolution type indicates that the import package, require
       * bundle or require capability must be resolved when the bundle is
       * resolved. If such an import, require bundle or require capability cannot
       * be resolved, the module fails to resolve.
       *
       * The directive value is encoded in the Import-Package, Require-Bundle or
       * Require-Capability manifest header like:
       *
       * `
       * Import-Package: org.osgi.framework; resolution:="mandatory"
       *
       * Require-Bundle: com.acme.module.test; resolution:="mandatory"
       *
       * Require-Capability: com.acme.capability; resolution:="mandatory"
       * `
       *
       * @see RESOLUTION_DIRECTIVE
       * @since 1.3
       */
      const val RESOLUTION_MANDATORY = "mandatory"

      /**
       * Manifest header directive value identifying an optional resolution type.
       * An optional resolution type indicates that the import, require bundle or
       * require capability is optional and the bundle may be resolved without the
       * import, require bundle or require capability being resolved. If the
       * import, require bundle or require capability is not resolved when the
       * bundle is resolved, the import, require bundle or require capability may
       * not be resolved until the bundle is refreshed.
       *
       * The directive value is encoded in the Import-Package, Require-Bundle or
       * Require-Capability manifest header like:
       *
       * `
       * Import-Package: org.osgi.framework; resolution:="optional"
       *
       * Require-Bundle: com.acme.module.test; resolution:="optional"
       *
       * Require-Capability: com.acme.capability; resolution:="optional"
       * `
       *
       * @see RESOLUTION_DIRECTIVE
       * @since 1.3
       */
      const val RESOLUTION_OPTIONAL = "optional"

      /**
       * Manifest header directive identifying a list of packages that an exported
       * package or provided capability uses.
       *
       * The directive value is encoded in the Export-Package or
       * Provide-Capability manifest header like:
       *
       * `
       * Export-Package: org.osgi.util.tracker; uses:="org.osgi.framework"
       *
       * Provide-Capability: com.acme.capability; uses:="com.acme.service"
       * `
       *
       * @see EXPORT_PACKAGE
       * @see PROVIDE_CAPABILITY
       * @since 1.3
       */
      const val USES_DIRECTIVE = "uses"

      /**
       * Manifest header directive identifying a list of classes to include in the
       * exported package.
       *
       * This directive is used by the Export-Package manifest header to identify
       * a list of classes of the specified package which must be allowed to be
       * exported. The directive value is encoded in the Export-Package manifest
       * header like:
       *
       *`
       * Export-Package: org.osgi.framework; include:="MyClass*"
       *`
       *
       * This directive is also used by the Bundle-ActivationPolicy manifest
       * header to identify the packages from which class loads will trigger lazy
       * activation. The directive value is encoded in the Bundle-ActivationPolicy
       * manifest header like:
       *
       * `
       * Bundle-ActivationPolicy: lazy; include:="org.osgi.framework"
       * `
       *
       * @see EXPORT_PACKAGE
       * @see BUNDLE_ACTIVATIONPOLICY
       * @since 1.3
       */
      const val INCLUDE_DIRECTIVE = "include"

      /**
       * Manifest header directive identifying a list of classes to exclude in the
       * exported package.
       *
       * This directive is used by the Export-Package manifest header to identify
       * a list of classes of the specified package which must not be allowed to
       * be exported. The directive value is encoded in the Export-Package
       * manifest header like:
       *
       *`
       * Export-Package: org.osgi.framework; exclude:="*Impl"
       *`
       *
       * This directive is also used by the Bundle-ActivationPolicy manifest
       * header to identify the packages from which class loads will not trigger
       * lazy activation. The directive value is encoded in the
       * Bundle-ActivationPolicy manifest header like:
       *
       * `
       * Bundle-ActivationPolicy: lazy; exclude:="org.osgi.framework"
       * `
       *
       * @see EXPORT_PACKAGE
       * @see BUNDLE_ACTIVATIONPOLICY
       * @since 1.3
       */
      const val EXCLUDE_DIRECTIVE = "exclude"

      /**
       * Manifest header directive identifying the visibility of a required bundle
       * in the Require-Bundle manifest header. The default value is
       * [private][VISIBILITY_PRIVATE].
       *
       * The directive value is encoded in the Require-Bundle manifest header
       * like:
       *
       * `
       * Require-Bundle: com.acme.module.test; visibility:="reexport"
       * `
       *
       * @see REQUIRE_BUNDLE
       * @see VISIBILITY_PRIVATE
       * @see VISIBILITY_REEXPORT
       * @since 1.3
       */
      const val VISIBILITY_DIRECTIVE = "visibility"

      /**
       * Manifest header directive value identifying a private visibility type. A
       * private visibility type indicates that any packages that are exported by
       * the required bundle are not made visible on the export signature of the
       * requiring bundle.
       *
       * The directive value is encoded in the Require-Bundle manifest header
       * like:
       *
       * `
       * Require-Bundle: com.acme.module.test; visibility:="private"
       * `
       *
       * @see VISIBILITY_DIRECTIVE
       * @since 1.3
       */
      const val VISIBILITY_PRIVATE = "private"

      /**
       * Manifest header directive value identifying a reexport visibility type. A
       * reexport visibility type indicates any packages that are exported by the
       * required bundle are re-exported by the requiring bundle. Any arbitrary
       * matching attributes with which they were exported by the required bundle
       * are deleted.
       *
       * The directive value is encoded in the Require-Bundle manifest header
       * like:
       *
       * `
       * Require-Bundle: com.acme.module.test; visibility:="reexport"
       * `
       *
       * @see VISIBILITY_DIRECTIVE
       * @since 1.3
       */
      const val VISIBILITY_REEXPORT = "reexport"

      /**
       * Manifest header identifying the bundle's activation policy.
       *
       * The header value may be retrieved from the `Dictionary` object
       * returned by the `Bundle.getHeaders` method.
       *
       * @see ACTIVATION_LAZY
       * @see INCLUDE_DIRECTIVE
       * @see EXCLUDE_DIRECTIVE
       * @since 1.4
       */
      const val BUNDLE_ACTIVATIONPOLICY = "Bundle-ActivationPolicy"

      /**
       * Bundle activation policy declaring the bundle must be activated when the
       * first class load is made from the bundle.
       *
       * A bundle with the lazy activation policy that is started with the
       * [START_ACTIVATION_POLICY][Bundle.START_ACTIVATION_POLICY] option
       * will wait in the [STARTING][Bundle.STARTING] state until the first
       * class load from the bundle occurs. The bundle will then be activated
       * before the class is returned to the requester.
       *
       * The activation policy value is specified as in the
       * Bundle-ActivationPolicy manifest header like:
       *
       * `
       * Bundle-ActivationPolicy: lazy
       * `
       *
       * @see BUNDLE_ACTIVATIONPOLICY
       * @see Bundle.start
       * @see Bundle.START_ACTIVATION_POLICY
       * @since 1.4
       */
      const val ACTIVATION_LAZY = "lazy"
    }
  }
}