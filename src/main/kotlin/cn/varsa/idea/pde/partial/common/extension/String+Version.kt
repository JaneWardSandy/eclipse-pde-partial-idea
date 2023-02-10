package cn.varsa.idea.pde.partial.common.extension

import cn.varsa.idea.pde.partial.common.version.Version
import cn.varsa.idea.pde.partial.common.version.VersionRange

fun String?.parseVersion(): Version = if (this != null) Version.parse(unquote()) else Version.emptyVersion

fun String?.parseVersionRange(): VersionRange =
  if (this != null) VersionRange.parse(unquote()) else VersionRange.anyVersionRange
