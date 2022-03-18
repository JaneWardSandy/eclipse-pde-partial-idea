package cn.varsa.idea.pde.partial.common.support

import org.osgi.framework.*

fun String?.parseVersionRange(): VersionRange =
    this?.unquote()?.takeIf(String::isNotBlank)?.let(::VersionRange) ?: VersionRangeAny

fun String?.parseVersion(): Version =
    this?.unquote()?.takeIf(String::isNotBlank)?.let(Version::parseVersion) ?: Version.emptyVersion

infix operator fun VersionRange?.contains(version: Version?): Boolean =
    this != null && (version == null || includes(version))

object VersionRangeAny : VersionRange(Version.emptyVersion.toString()) {
    override fun includes(version: Version?): Boolean = true
}
