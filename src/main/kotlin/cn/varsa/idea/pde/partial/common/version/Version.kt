package cn.varsa.idea.pde.partial.common.version

import java.util.*

class Version(
  val major: Int,
  val minor: Int = 0,
  val micro: Int = 0,
  val qualifier: String = "",
) : Comparable<Version> {

  companion object Factory {
    private const val separator = "."

    val emptyVersion = Version(0)

    operator fun invoke(version: String): Version {
      if (version.isBlank()) return emptyVersion

      val trimVersion = version.trim()
      val major: Int
      var minor = 0
      var micro = 0
      var qualifier = ""

      try {
        val tokenizer = StringTokenizer(trimVersion, separator)
        major = parseInt(tokenizer.nextToken(), trimVersion)

        if (tokenizer.hasMoreTokens()) {
          minor = parseInt(tokenizer.nextToken(), trimVersion)

          if (tokenizer.hasMoreTokens()) {
            micro = parseInt(tokenizer.nextToken(), trimVersion)

            if (tokenizer.hasMoreTokens()) {
              qualifier = tokenizer.nextToken("") // remaining string

              require(!tokenizer.hasMoreTokens()) { "Invalid version \"$trimVersion\": invalid format" }
            }
          }
        }
      } catch (e: NoSuchElementException) {
        throw IllegalArgumentException("Invalid version \"$trimVersion\": invalid format", e)
      }

      return Version(major, minor, micro, qualifier)
    }

    private fun parseInt(value: String, version: String): Int = try {
      value.toInt()
    } catch (e: NumberFormatException) {
      throw IllegalArgumentException("Invalid version \"$version\": non-numeric or negative number \"$value\"", e)
    }
  }

  init {
    validate()
  }

  private fun validate() {
    require(major >= 0) { "Invalid version \"$this\": negative number \"$major\"" }
    require(minor >= 0) { "Invalid version \"$this\": negative number \"$minor\"" }
    require(micro >= 0) { "Invalid version \"$this\": negative number \"$micro\"" }
    require(qualifier.all { ch -> ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '-' || ch == '_' }) { "Invalid version \"$this\": invalid qualifier \"$qualifier\"" }
  }

  override fun toString(): String =
    "$major$separator$minor$separator$micro${if (qualifier.isNotBlank()) "$separator$qualifier" else ""}"

  override fun hashCode(): Int =
    listOf(17, major, minor, micro, qualifier.hashCode()).fold(1) { acc, next -> 31 * acc + next }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Version) return false

    if (major != other.major) return false
    if (minor != other.minor) return false
    if (micro != other.micro) return false
    if (qualifier != other.qualifier) return false
    return true
  }

  override fun compareTo(other: Version): Int {
    if (this == other) return 0

    var diff = major - other.major
    if (diff != 0) return diff

    diff = minor - other.minor
    if (diff != 0) return diff

    diff = micro - other.micro
    if (diff != 0) return diff

    return qualifier.compareTo(other.qualifier)
  }
}