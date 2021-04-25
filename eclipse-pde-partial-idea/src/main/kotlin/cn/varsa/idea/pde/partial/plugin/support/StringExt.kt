package cn.varsa.idea.pde.partial.plugin.support

fun String.equalAny(vararg others: String, ignoreCase: Boolean = false): Boolean =
    others.any { it.equals(this, ignoreCase) }
