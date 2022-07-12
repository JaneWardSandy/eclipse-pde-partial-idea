@file:JvmName("PartialConstants")

package cn.varsa.idea.pde.partial.common.constants

const val MANIFEST_MF = "MANIFEST.MF"
const val META_INF = "META-INF"
const val MANIFEST_PATH = "$MANIFEST_MF/$META_INF"
const val PLUGINS_XML = "plugin.xml"
const val FRAGMENT_XML = "fragment.xml"
const val FEATURE_XML = "feature.xml"
const val BUILD_PROPERTIES = "build.properties"

const val FEATURES = "features"
const val DROPINS = "dropins"
const val PLUGINS = "plugins"
const val ARTIFACTS = "artifacts"

const val PARTIAL_PREFIX = "Partial: "
const val ARTIFACT_PREFIX = PARTIAL_PREFIX
const val MODULE_LIBRARY_NAME = "Partial-Runtime"
const val MODULE_COMPILE_ONLY_LIBRARY_NAME = "Partial-CompileOnly"
const val PROJECT_LIBRARY_NAME_PREFIX = PARTIAL_PREFIX

const val KOTLIN_BUNDLE_SYMBOL_NAME = "org.jetbrains.kotlin.osgi-bundle"
const val KOTLIN_ORDER_ENTRY_NAME = "KotlinJavaRuntime"

const val KILOBYTE = 1024
const val MEGABYTE = KILOBYTE * KILOBYTE

const val JAVA = "java"
const val KOTLIN = "kotlin"

const val SYSTEM_BUNDLE = "system.bundle"
const val ORG_ECLIPSE_OSGI = "org.eclipse.osgi"
