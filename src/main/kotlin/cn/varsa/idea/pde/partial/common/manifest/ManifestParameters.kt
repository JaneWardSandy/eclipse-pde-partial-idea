package cn.varsa.idea.pde.partial.common.manifest

import cn.varsa.idea.pde.partial.common.util.*

data class ManifestParameters(val attributes: Map<String, ParameterAttributes>) {

  companion object Factory {
    fun parse(value: String): ManifestParameters {
      val attributes = mutableMapOf<String, ParameterAttributes>()

      val qt = QuotedTokenizer(value, ";=,")
      var del: Char

      do {
        val attribute = mutableMapOf<String, String>()
        val directive = mutableMapOf<String, String>()
        val aliases = mutableListOf<String>()
        val name = qt.nextToken(",;")

        del = qt.separator
        if (name.isNullOrBlank()) {
          if (name == null) break
        } else {
          aliases += name

          while (del == ';') {
            val adName = qt.nextToken()
            if (qt.separator.also { del = it } != '=') {
              if (!adName.isNullOrBlank()) aliases += adName
            } else {
              val adValue = qt.nextToken() ?: ""
              del = qt.separator

              if (adName.isNullOrBlank()) continue
              if (adName.endsWith(':')) {
                directive[adName.dropLast(1)] = adValue
              } else {
                attribute[adName] = adValue
              }
            }
          }

          val attrs = ParameterAttributes(attribute, directive)
          aliases.forEach { attributes[it] = attrs }
        }
      } while (del == ',')

      return ManifestParameters(attributes)
    }
  }

  override fun toString(): String =
    attributes.entries.joinToString(",${System.lineSeparator()}") { (key, attrs) -> "$key$attrs" }
}
