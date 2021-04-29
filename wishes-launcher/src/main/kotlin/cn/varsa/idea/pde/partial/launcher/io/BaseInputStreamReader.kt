package cn.varsa.idea.pde.partial.launcher.io

import java.io.*
import java.nio.charset.*

class BaseInputStreamReader(
    private val inputStream: InputStream,
    charset: Charset = Charset.defaultCharset(),
) : InputStreamReader(inputStream, charset) {

    override fun close() {
        inputStream.close()
    }
}
