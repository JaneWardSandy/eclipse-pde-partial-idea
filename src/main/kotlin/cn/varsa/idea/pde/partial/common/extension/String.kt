package cn.varsa.idea.pde.partial.common.extension

fun CharSequence.surroundingWith(char: Char, ignoreCase: Boolean = false): Boolean =
  indexOf(char) > -1 && length >= 2 && startsWith(char, ignoreCase) && endsWith(char, ignoreCase)

fun CharSequence.surroundingWith(cs: CharSequence, ignoreCase: Boolean = false): Boolean =
  length >= 2 * cs.length && startsWith(cs, ignoreCase) && endsWith(cs, ignoreCase)

fun String.equalAny(vararg others: String, ignoreCase: Boolean = false): Boolean = when {
  others.isEmpty() -> false
  !ignoreCase -> this in others
  else -> others.any { it.equals(this, true) }
}

fun String.unquote(): String = when {
  length < 2 -> this
  indexOf('\'') < 0 && indexOf('"') < 0 && indexOf('`') < 0 -> this
  surroundingWith('\'') || surroundingWith('"') || surroundingWith('`') -> substring(1, length - 1)
  else -> this
}
