package cn.varsa.idea.pde.partial.common.support

inline fun Boolean.ifTrue(block: () -> Unit): Boolean {
    if (this) block()
    return this
}

inline fun Boolean.ifFalse(block: () -> Unit): Boolean {
    if (this.not()) block()
    return this
}

inline fun <R> Boolean.runTrue(block: () -> R): R? = if (this) block() else null
inline fun <R> Boolean.runFalse(block: () -> R): R? = if (!this) block() else null
