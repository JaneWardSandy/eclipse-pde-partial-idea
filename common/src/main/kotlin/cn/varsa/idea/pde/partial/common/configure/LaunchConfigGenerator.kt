package cn.varsa.idea.pde.partial.common.configure

import cn.varsa.idea.pde.partial.common.service.*
import cn.varsa.idea.pde.partial.common.support.*
import org.osgi.framework.*
import java.io.*
import java.util.*

object LaunchConfigGenerator {
    private val PATTERN_BUNDLE_PATH_POSTFIX =
        "(_\\d+(?<!x86_64|ia64_32)(\\.\\d+(\\.\\d+(\\.[a-zA-Z0-9_-]+)?)?)?(\\.\\w+)?$)|(\\.(?:jar|war|zip)$)".toRegex(
            RegexOption.IGNORE_CASE
        )

    /**
     * Generate config.ini file contents
     * @param configService config store service
     * @param properties product's config.ini file in %installArea%/configuration/config.ini
     * @see cn.varsa.idea.pde.partial.common.service.ConfigService#installArea
     */
    fun storeConfigIniFile(configService: ConfigService, properties: Properties = Properties()) {
        configService.configurationDirectory.makeDirs()

        if (configService.product != properties["eclipse.product"] && configService.application != properties["eclipse.application"]) properties.clear()

        properties.putIfAbsent("eclipse.product", configService.product)
        properties.putIfAbsent("eclipse.application", configService.application)
        properties.putIfAbsent("osgi.install.area", configService.installArea.protocolUrl)
        properties.putIfAbsent("osgi.instance.area.default", configService.instanceArea.protocolUrl)
        properties.putIfAbsent("osgi.configuration.cascaded", false.toString())
        properties.putIfAbsent("osgi.framework", "org.eclipse.osgi")
        properties.putIfAbsent("osgi.bundles.defaultStartLevel", "4")

        properties["org.eclipse.equinox.simpleconfigurator.configUrl"] = configService.bundlesInfoFile.protocolUrl
        properties["eclipse.p2.data.area"] = "@config.dir/.p2"
        properties["osgi.bundles"] = "org.eclipse.equinox.simpleconfigurator@1:start"
        properties["org.eclipse.update.reconcile"] = "false"


        val bundleUrlPath = { name: String ->
            configService.libraries.firstOrNull { it.name == name || configService.getManifest(it)?.bundleSymbolicName?.key == name }?.protocolUrl
        }

        stripPathInformation(properties.getProperty("osgi.framework"), configService).substringBefore('@')
            .let(bundleUrlPath)?.also { properties["osgi.framework"] = it }

        stripPathInformation(properties.getProperty("osgi.splashPath"), configService).substringBefore('@')
            .let(bundleUrlPath)?.also { properties["osgi.splashPath"] = it }

        properties.getProperty("osgi.framework.extensions")?.splitToSequence(',')
            ?.map { stripPathInformation(it, configService) }?.map { it.substringBefore('@') }
            ?.mapNotNull(bundleUrlPath)?.joinToString(",")?.also { properties["osgi.framework.extensions"] = it }

        properties.getProperty("osgi.bundles").splitToSequence(',').map { stripPathInformation(it, configService) }
            .mapNotNull {
                val name = it.substringBefore('@')
                bundleUrlPath(name)?.run { it.replace(name, this) }
            }.joinToString(",").also { properties["osgi.bundles"] = it }

        configService.configIniFile.touchFile().outputStream().use { properties.store(it, "Configuration File") }
    }

    /**
     * Generate dev.properties file contents
     * @param configService config store service
     */
    fun storeDevProperties(configService: ConfigService) {
        val properties = Properties()

        configService.devModules.forEach {
            properties[it.bundleSymbolicName] = it.compilerClassRelativePathToModule.joinToString(separator = ",")
        }
        properties["@ignoredot@"] = "true"

        configService.devPropertiesFile.touchFile().outputStream().use { properties.store(it, "Development File") }
    }

    /**
     * Generate bundles.info file contents
     * @param configService config store service
     */
    fun storeBundleInfo(configService: ConfigService) {
        configService.bundlesInfoFile.touchFile().bufferedWriter().use { writer ->
            writer.appendLine("#version=1")

            val appendBundleLine: (String, Version?, File, Int?) -> Unit = { name, version, file, level ->
                writer.append(name).append(',').append(version?.toString() ?: Version.emptyVersion.toString())
                    .append(',').append(file.protocolUrl).append(',')
                    .append(level?.toString() ?: configService.startUpLevel(name).toString()).append(',')
                    .appendLine(configService.isAutoStartUp(name).toString())
            }

            configService.libraries.forEach { bundle ->
                configService.getManifest(bundle)?.also {
                    appendBundleLine(it.bundleSymbolicName?.key ?: bundle.nameWithoutExtension,
                                     it.bundleVersion,
                                     bundle,
                                     it.eclipseSourceBundle?.let { -1 })
                }
            }

            configService.devModules.forEach { module ->
                val moduleDirectory = File(configService.projectDirectory, module.relativePathToProject)
                configService.getManifest(moduleDirectory)?.also {
                    appendBundleLine(module.bundleSymbolicName, it.bundleVersion, moduleDirectory, null)
                }
            }
        }
    }

    private fun stripPathInformation(osgiBundles: String, configService: ConfigService): String =
        osgiBundles.splitToSequence(',').map { it.replace("\\\\:|/:".toRegex(), ":") }.map { token ->
            val bundle = token.substringBefore('@').trim().substringAfter("reference:").substringAfter("platform:")
                .substringAfter("file:")

            val file = File(bundle)
            var id: String? = null

            if (file.isAbsolute) id = try {
                if (file.isDirectory || (file.isFile && file.extension.lowercase() == "jar")) {
                    configService.getManifest(file)?.bundleSymbolicName?.key
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
            if (id == null) id = file.name
            if (id != null) id = PATTERN_BUNDLE_PATH_POSTFIX.replaceFirst(id, "")

            (id ?: bundle) + if (token.contains('@')) '@' + token.substringAfter('@') else ""
        }.joinToString(",")
}
