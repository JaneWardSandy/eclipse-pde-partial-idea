package cn.varsa.idea.pde.partial.common.extension

import cn.varsa.idea.pde.partial.common.version.*

fun String?.parseVersion(): Version = if (this != null) Version.parse(unquote()) else Version.EMPTY_VERSION

fun String?.parseVersionRange(): VersionRange =
  if (this != null) VersionRange.parse(unquote()) else VersionRange.ANY_VERSION_RANGE
