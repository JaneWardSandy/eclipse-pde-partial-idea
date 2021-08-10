package cn.varsa.idea.pde.partial.common.support

fun String.equalAny(vararg others: String, ignoreCase: Boolean = false): Boolean =
    others.any { it.equals(this, ignoreCase) }

fun String.unquote(): String = when {
    indexOf('\'') < 0 && indexOf('\"') < 0 && indexOf('`') < 0 -> this
    length < 2 -> this
    startsWith('\'') && endsWith('\'') -> substring(1, length - 1)
    startsWith('\"') && endsWith('\"') -> substring(1, length - 1)
    startsWith('`') && endsWith('`') -> substring(1, length - 1)
    else -> this
}
