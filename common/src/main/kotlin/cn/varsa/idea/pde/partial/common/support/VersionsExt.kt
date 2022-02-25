package cn.varsa.idea.pde.partial.common.support

import org.osgi.framework.*

fun String?.parseVersionRange(): VersionRange = if (this == null) VersionRangeAny else VersionRange(unquote())
fun String?.parseVersion(): Version = if (this == null) Version.emptyVersion else Version.parseVersion(unquote())

infix operator fun VersionRange?.contains(version: Version?): Boolean =
    this != null && (version == null || includes(version))

object VersionRangeAny : VersionRange(Version.emptyVersion.toString()) {
    override fun includes(version: Version?): Boolean = true
}
