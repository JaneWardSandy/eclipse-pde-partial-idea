package cn.varsa.idea.pde.partial.common.service

import cn.varsa.idea.pde.partial.common.domain.*
import java.io.*

/**
 * Store platform launcher information
 */
interface ConfigService {
    val product: String
    val application: String

    /**
     * Runtime resource will store into this directory
     */
    val dataPath: File

    /**
     * Eclipse product/application root directory.
     *
     * Most would be:
     * - %TC_ROOT%/portal: parent directory of teamcenter.exe
     * - Eclipse: parent directory of plugins directory(Linux/Mac), or eclipse.exe(Windows)
     */
    val installArea: File
    val instanceArea: File
        get() = dataPath
    val configurationDirectory: File
        get() = File(dataPath, "config")

    val configIniFile: File
        get() = File(configurationDirectory, "config.ini")
    val devPropertiesFile: File
        get() = File(configurationDirectory, "dev.properties")
    val bundlesInfoFile: File
        get() = File(configurationDirectory, "org.eclipse.equinox.simpleconfigurator/bundles.info")

    val projectDirectory: File

    val libraries: List<File>
    val devModules: List<DevModule>

    fun getManifest(jarFileOrDirectory: File): BundleManifest?
    fun startUpLevel(bundleSymbolicName: String): Int
    fun isAutoStartUp(bundleSymbolicName: String): Boolean
}
