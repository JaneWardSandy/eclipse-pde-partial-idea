package cn.varsa.idea.pde.partial.common.version

import cn.varsa.idea.pde.partial.common.extension.*
import java.util.*

class VersionRange(
  val leftClosed: Boolean = true,
  val left: Version,
  val right: Version? = null,
  val rightClosed: Boolean = false,
) {

  companion object Factory {
    private const val leftOpen = "("
    private const val leftClose = "["
    private const val leftDelimiters = "$leftOpen$leftClose"
    private const val rightOpen = ")"
    private const val rightClose = "]"
    private const val rightDelimiters = "$rightOpen$rightClose"
    private const val endpointDelimiter = ","

    val anyVersionRange: VersionRange = VersionRange(true, Version.emptyVersion)

    fun parse(range: String): VersionRange {
      var closedLeft: Boolean
      val closedRight: Boolean
      val endpointLeft: Version
      val endpointRight: Version?

      try {
        val tokenizer = StringTokenizer(range, leftDelimiters, true)
        var token = tokenizer.nextToken().trim() // whitespace or left delim
        if (token.isEmpty()) token = tokenizer.nextToken() // leading whitespace, goto left delim

        closedLeft = leftClose == token
        if (!closedLeft && leftOpen != token) {
          // first token is not a delimiter, so it must be "at-least"
          // there must be no more tokens
          require(!tokenizer.hasMoreTokens()) { "Invalid range \"$range\": invalid format" }

          closedLeft = true
          closedRight = false
          endpointLeft = token.parseVersion()
          endpointRight = null
        } else {
          endpointLeft = tokenizer.nextToken(endpointDelimiter).parseVersion()
          tokenizer.nextToken() // consume comma
          endpointRight = tokenizer.nextToken(rightDelimiters).parseVersion()
          token = tokenizer.nextToken() // right delim

          closedRight = rightClose == token
          require(closedRight || rightOpen == token) { "Invalid range \"$range\": invalid format" }

          if (tokenizer.hasMoreTokens()) { // any more tokens have to be whitespace
            token = tokenizer.nextToken("").trim()

            // trailing whitespace
            require(token.isEmpty()) { "Invalid range \"$range\": invalid format" }
          }
        }
      } catch (e: NoSuchElementException) {
        throw IllegalArgumentException("Invalid range \"$range\": invalid format", e)
      } catch (e: IllegalArgumentException) {
        throw if (e.message?.startsWith("Invalid range ") == true) e
        else IllegalArgumentException("Invalid range \"$range\": ${e.message}", e)
      }

      return VersionRange(closedLeft, endpointLeft, endpointRight, closedRight)
    }
  }

  fun includes(version: Version): Boolean = when {
    isEmpty() -> false
    left.compareTo(version) >= if (leftClosed) 1 else 0 -> false
    right == null -> true
    else -> right.compareTo(version) >= if (rightClosed) 0 else 1
  }

  fun isEmpty(): Boolean = if (right == null) false
  else {
    val comparison = left.compareTo(right)
    if (comparison == 0) !leftClosed || !rightClosed else comparison > 0
  }

  fun isExact(): Boolean = if (isEmpty() || right == null) false
  else if (leftClosed) {
    if (rightClosed) left == right // [l,r]: exact if l == r
    else "$left-".parseVersion() >= right // [l,r): exact if l++ >= r
  } else {
    if (rightClosed) "$left-".parseVersion() == right // (l,r] is equivalent to [l++,r]: exact if l++ == r
    else "$left--".parseVersion() >= right // (l,r) is equivalent to [l++,r): exact if (l++)++ >=r
  }

  override fun toString(): String = if (right == null) left.toString()
  else buildString {
    if (leftClosed) append(leftClose) else append(leftOpen)
    append(left)
    append(endpointDelimiter)
    append(right)
    if (rightClosed) append(rightClose) else append(rightOpen)
  }

  override fun hashCode(): Int = if (isEmpty()) 31
  else {
    var hash = 31 * if (leftClosed) 7 else 5
    hash = 31 * hash + left.hashCode()
    if (right != null) {
      hash = 31 * hash + right.hashCode()
      hash = 31 * hash + if (rightClosed) 7 else 5
    }
    hash
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VersionRange) return false

    if (isEmpty() && other.isEmpty()) return true
    if (right == null) return leftClosed == other.leftClosed && other.right == null && left == other.left
    return leftClosed == other.leftClosed && rightClosed == other.rightClosed && left == other.left && right == other.right
  }

  infix operator fun contains(version: Version?): Boolean = version == null || includes(version)
}
