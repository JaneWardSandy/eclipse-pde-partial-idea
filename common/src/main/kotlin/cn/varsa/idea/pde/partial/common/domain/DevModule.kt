package cn.varsa.idea.pde.partial.common.domain

import java.io.*

/**
 * Resolve plug-ins as source, and add it into eclipse platform dev.properties.
 */
data class DevModule(
    /**
     * Relative path for module from project directory.
     */
    val relativePathToProject: String,

    /**
     * Bundle symbolic name in manifest.mf.
     */
    val bundleSymbolicName: String,

    /**
     * Relative path for compiled class root form module directory.
     */
    val compilerClassRelativePathToModule: List<String>,
) : Serializable
