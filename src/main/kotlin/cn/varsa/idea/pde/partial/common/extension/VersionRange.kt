package cn.varsa.idea.pde.partial.common.extension

import cn.varsa.idea.pde.partial.common.version.*
import kotlin.internal.*

@InlineOnly inline fun String?.parseVersionRange(): VersionRange =
  if (this != null) VersionRange.parse(unquote()) else VersionRange.anyVersionRange
