package cn.varsa.idea.pde.partial.common.support

fun String.equalAny(vararg others: String, ignoreCase: Boolean = false): Boolean =
    others.any { it.equals(this, ignoreCase) }

fun CharSequence.surroundingWith(char: Char, ignoreCase: Boolean = false): Boolean =
    indexOf(char) > -1 && length >= 2 && startsWith(char, ignoreCase) && endsWith(char, ignoreCase)

fun CharSequence.surroundingWith(cs: CharSequence, ignoreCase: Boolean = false): Boolean =
    length >= 2 * cs.length && startsWith(cs, ignoreCase) && endsWith(cs, ignoreCase)

fun String.unquote(): String = when {
    indexOf('\'') < 0 && indexOf('\"') < 0 && indexOf('`') < 0 -> this
    length < 2 -> this
    surroundingWith('\'') || surroundingWith('\"') || surroundingWith('`') -> substring(1, length - 1)
    else -> this
}
