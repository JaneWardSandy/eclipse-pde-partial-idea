package cn.varsa.idea.pde.partial.plugin.support

import com.intellij.util.io.*
import com.intellij.util.io.DataInputOutputUtil.*
import java.io.*

fun DataInput.readString(): String = IOUtil.readUTF(this)
fun DataOutput.writeString(string: String) = IOUtil.writeUTF(this, string)

fun DataInput.readStringList(): List<String> = readSeq(this) { readString() }
fun DataOutput.writeStringList(iterable: Iterable<String>) = writeSeq(this, iterable.toList()) { writeString(it) }

fun <T : Any> DataOutput.writeNullable(nullable: T?, writeT: DataOutput.(T) -> Unit) {
    writeBoolean(nullable != null)
    nullable?.let { writeT(it) }
}

fun <T : Any> DataInput.readNullable(readT: DataInput.() -> T): T? {
    val hasValue = readBoolean()
    return if (hasValue) readT() else null
}
