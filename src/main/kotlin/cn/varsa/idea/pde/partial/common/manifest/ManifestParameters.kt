package cn.varsa.idea.pde.partial.common.manifest

import cn.varsa.idea.pde.partial.common.util.*

class ManifestParameters(value: String) {
  private val _attributes: MutableMap<String, ParameterAttributes> = mutableMapOf()
  val attributes: Map<String, ParameterAttributes> = _attributes

  init {
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
        aliases.forEach { _attributes[it] = attrs }
      }
    } while (del == ',')
  }
}
