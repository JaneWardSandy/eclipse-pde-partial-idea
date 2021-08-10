package cn.varsa.idea.pde.partial.plugin.manifest.lang

import org.jetbrains.lang.manifest.header.*
import org.osgi.framework.Constants.*

class ManifestHeaderParserProvider : HeaderParserProvider {
    private val parsers = mutableMapOf<String, HeaderParser>()
    override fun getHeaderParsers(): MutableMap<String, HeaderParser> = parsers

    init {
        parsers[BUNDLE_MANIFESTVERSION] = BundleManifestVersionParser
        parsers[BUNDLE_NAME] = NotBlankValueParser
        parsers[BUNDLE_SYMBOLICNAME] = BundleSymbolicNameParser
        parsers[BUNDLE_VERSION] = BundleVersionParser
        parsers[BUNDLE_ACTIVATOR] = BundleActivatorParser
        parsers[BUNDLE_VENDOR] = NotBlankValueParser
        parsers[REQUIRE_BUNDLE] = RequireBundleParser
        parsers[BUNDLE_REQUIREDEXECUTIONENVIRONMENT] = RequiredExecutionEnvironmentParser
        parsers[BUNDLE_ACTIVATIONPOLICY] = BundleActivationPolicyParser
        parsers[BUNDLE_CLASSPATH] = BundleClasspathParser
        parsers[IMPORT_PACKAGE] = ImportPackageParser
        parsers[EXPORT_PACKAGE] = ExportPackageParser
    }
}
