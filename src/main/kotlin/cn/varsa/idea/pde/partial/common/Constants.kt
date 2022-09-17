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

  object OSGI {
    const val SYSTEM_BUNDLE = "system.bundle"
    const val ORG_ECLIPSE_OSGI = "org.eclipse.osgi"

    object Eclipse {
      const val ECLIPSE_EXTENSIBLE_API = "Eclipse-ExtensibleAPI"
      const val ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle"
    }

    object Header {
      /**
       * Manifest header identifying the symbolic names of other bundles required
       * by the bundle.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       *
       * @since 1.3
       */
      const val REQUIRE_BUNDLE = "Require-Bundle"

      /**
       * Manifest header identifying the packages on which the bundle depends.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val IMPORT_PACKAGE = "Import-Package"

      /**
       * Manifest header identifying the packages that the bundle offers to the
       * Framework for export.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val EXPORT_PACKAGE = "Export-Package"

      /**
       * Manifest header identifying a list of directories and embedded JAR files,
       * which are bundle resources used to extend the bundle's classpath.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_CLASSPATH = "Bundle-ClassPath"

      /**
       * Manifest header identifying the bundle's activator class.
       *
       * <p>
       * If present, this header specifies the name of the bundle resource class
       * that implements the {@code BundleActivator} interface and whose
       * {@code start} and {@code stop} methods are called by the Framework when
       * the bundle is started and stopped, respectively.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_ACTIVATOR = "Bundle-Activator"

      /**
       * Manifest header identifying the required execution environment for the
       * bundle. The service platform may run this bundle if any of the execution
       * environments named in this header matches one of the execution
       * environments it implements.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       *
       * @since 1.2
       * @deprecated As of 1.6. Replaced by the {@code osgi.ee} capability.
       */
      const val BUNDLE_REQUIREDEXECUTIONENVIRONMENT = "Bundle-RequiredExecutionEnvironment"

      /**
       * Manifest header identifying the bundle's symbolic name.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       *
       * @since 1.3
       */
      const val BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName"

      /**
       * Manifest header identifying the symbolic name of another bundle for which
       * that the bundle is a fragment.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       *
       * @since 1.3
       */
      const val FRAGMENT_HOST = "Fragment-Host"

      /**
       * Manifest header identifying the bundle's version.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_VERSION = "Bundle-Version"

      /**
       * Manifest header identifying the capabilities on which the bundle depends.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       *
       * @since 1.6
       */
      const val REQUIRE_CAPABILITY = "Require-Capability"

      /**
       * Manifest header identifying the capabilities that the bundle offers to
       * provide to other bundles.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       *
       * @since 1.6
       */
      const val PROVIDE_CAPABILITY = "Provide-Capability"

      /**
       * Manifest header identifying the bundle's name.
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_NAME = "Bundle-Name"

      /**
       * Manifest header containing a brief description of the bundle's
       * functionality.
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_DESCRIPTION = "Bundle-Description"

      /**
       * Manifest header identifying the bundle's copyright information.
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_COPYRIGHT = "Bundle-Copyright"

      /**
       * Manifest header identifying the bundle's documentation URL, from which
       * further information about the bundle may be obtained.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_DOCURL = "Bundle-DocURL"

      /**
       * Manifest header identifying the bundle's vendor.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_VENDOR = "Bundle-Vendor"

      /**
       * Manifest header identifying the contact address where problems with the
       * bundle may be reported; for example, an email address.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_CONTACTADDRESS = "Bundle-ContactAddress"

      /**
       * Manifest header identifying the bundle's category.
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_CATEGORY = "Bundle-Category"

      /**
       * Manifest header identifying a number of hardware environments and the
       * native language code libraries that the bundle is carrying for each of
       * these environments.
       *
       * <p>
       * The header value may be retrieved from the {@code Dictionary} object
       * returned by the {@code Bundle.getHeaders} method.
       */
      const val BUNDLE_NATIVECODE = "Bundle-NativeCode"
    }
  }
}