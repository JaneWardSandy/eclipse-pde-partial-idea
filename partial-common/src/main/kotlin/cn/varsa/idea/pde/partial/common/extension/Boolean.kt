package cn.varsa.idea.pde.partial.common.extension

import kotlin.contracts.*
import kotlin.internal.*

@InlineOnly inline fun Boolean.ifTrue(block: () -> Unit): Boolean {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

  if (this) block()
  return this
}

@InlineOnly inline fun Boolean.ifFalse(block: () -> Unit): Boolean {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

  if (this.not()) block()
  return this
}

@InlineOnly inline fun <R> Boolean.runIfTrue(block: () -> R): R? {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

  return if (this) block() else null
}

@InlineOnly inline fun <R> Boolean.runIfFalse(block: () -> R): R? {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

  return if (!this) block() else null
}
