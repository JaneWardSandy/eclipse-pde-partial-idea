package cn.varsa.idea.pde.partial.common.extension

import com.intellij.util.io.*
import java.io.*

fun DataInput.readString(): String = IOUtil.readUTF(this)
fun DataOutput.writeString(string: String) = IOUtil.writeUTF(this, string)
