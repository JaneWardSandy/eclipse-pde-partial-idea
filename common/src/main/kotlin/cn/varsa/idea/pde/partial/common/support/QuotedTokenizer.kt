package cn.varsa.idea.pde.partial.common.support

class QuotedTokenizer(
    private val string: String,
    private val separators: String,
    private val returnTokens: Boolean = false,
    private val retainQuotes: Boolean = false,
) {
    private var index = 0
    private var peek: String? = null
    var separator = 0.toChar()
        private set

    override fun toString() = "\"$string\" - \"$separators\" - $returnTokens"

    fun nextToken(separators: String = this.separators): String? {
        separator = 0.toChar()
        if (peek != null) return peek.also { peek = null }
        if (index == string.length) return null

        val sb = StringBuilder()
        var hadstring = false // means no further trimming
        var escaped = false // means previous char was backslash
        var lastNonWhitespace = 0
        while (index < string.length) {
            val c = string[index++]
            if (escaped) {
                sb.append(c)
                escaped = false
            } else {
                if (separators.contains(c)) {
                    if (returnTokens) {
                        peek = c.toString()
                    } else {
                        separator = c
                    }
                    break
                }

                if (c.isWhitespace()) {
                    if (index == string.length) {
                        break
                    }
                    if (sb.isNotEmpty()) {
                        sb.append(c)
                    }
                    continue
                }

                when (c) {
                    '"', '\'' -> {
                        hadstring = true
                        quotedString(sb, c)
                    }
                    '\\' -> {
                        escaped = true
                        sb.append(c)
                    }
                    else -> sb.append(c)
                }
            }

            lastNonWhitespace = sb.length
        }
        sb.setLength(lastNonWhitespace)
        val result = sb.toString()

        if (!hadstring && result.isEmpty() && index == string.length) return null
        return result
    }

    private fun quotedString(sb: StringBuilder, quote: Char) {
        val retain = retainQuotes || sb.isNotEmpty()
        if (retain) {
            sb.append(quote)
        }

        while (index < string.length) {
            var c = string[index++]
            if (c == quote) {
                if (retain) {
                    sb.append(quote)
                }
                break
            }
            if (c == '\\' && index < string.length) {
                c = string[index++]
                if (retain || c != quote) {
                    sb.append('\\')
                }
            }
            sb.append(c)
        }
    }
}
