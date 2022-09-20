package cn.varsa.idea.pde.partial.common.extension

import cn.varsa.idea.pde.partial.common.version.*
import kotlin.internal.*

@InlineOnly inline fun String?.parseVersion(): Version =
  if (this != null) Version.parse(unquote()) else Version.emptyVersion
