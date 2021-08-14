package cn.varsa.idea.pde.partial.common.support

import java.io.*
import java.net.*

val File.protocolUrl: String get() = toURI().toURL().toString()

fun String.toFile(): File = File(this)
fun String.toFile(child: String): File = File(this, child)
fun URI.toFile(): File = File(this)

fun File.touchFile(): File = this.apply {
    parentFile.makeDirs()
    if (!exists()) createNewFile()
}

fun File.makeDirs(): File = this.apply { if (!exists()) mkdirs() }
