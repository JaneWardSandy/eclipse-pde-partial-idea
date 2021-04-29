package cn.varsa.idea.pde.partial.launcher.io

import java.io.*
import java.nio.charset.*

abstract class BaseOutputReader(private val reader: Reader) : BaseDataReader() {
    open val splitToLines = true
    open val sendIncompleteLines = true
    open val withSeparators = true

    private val inputBuffer = CharArray(8192)
    private val lineBuffer = StringBuilder()
    private var carry = false

    constructor(
        inputStream: InputStream, charset: Charset = Charset.defaultCharset(),
    ) : this(BaseInputStreamReader(inputStream, charset))

    override fun readAvailable(): Boolean {
        var read = false

        try {
            var n = 0
            while (reader.ready() && reader.read(inputBuffer).also { n = it } >= 0) {
                if (n > 0) {
                    read = true
                    processInput(inputBuffer, lineBuffer, n)
                }
            }
        } finally {
            if (carry) {
                lineBuffer.append('\r')
                carry = false
            }
            if (lineBuffer.isNotBlank() && sendIncompleteLines) sendText(lineBuffer)
        }

        return read
    }

    override fun flush() {
        if (lineBuffer.isNotBlank()) sendText(lineBuffer)
    }

    private fun processInput(buffer: CharArray, line: StringBuilder, n: Int) {
        if (splitToLines) {
            var i = 0
            while (i < n) {
                var c: Char
                if (i == 0 && carry) {
                    c = '\r'
                    i--
                    carry = false
                } else {
                    c = buffer[i]
                }
                if (c == '\r') {
                    if (i + 1 == n) {
                        carry = true
                        i++
                        continue
                    } else if (buffer[i + 1] == '\n') {
                        i++
                        continue
                    }
                }
                if (c != '\n' || sendIncompleteLines || withSeparators) {
                    line.append(c)
                }
                if (c == '\n') {
                    sendText(line)
                }
                i++
            }
            if (line.isNotEmpty() && sendIncompleteLines) {
                sendText(line)
            }
        } else {
            onTextAvailable(String(buffer, 0, n))
        }
    }

    private fun sendText(line: StringBuilder) {
        val text = line.toString()
        line.setLength(0)
        onTextAvailable(text)
    }

    override fun close() {
        reader.close()
    }

    protected abstract fun onTextAvailable(text: String)
}
