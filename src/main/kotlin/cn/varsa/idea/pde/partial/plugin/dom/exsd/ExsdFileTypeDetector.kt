package cn.varsa.idea.pde.partial.plugin.dom.exsd

import com.intellij.ide.highlighter.*
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.util.io.*
import com.intellij.openapi.vfs.*

class ExsdFileTypeDetector : FileTypeRegistry.FileTypeDetector {
  override fun getDesiredContentPrefixLength(): Int = 0
  override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): FileType? =
    if (file.extension?.lowercase() == "exsd") XmlFileType.INSTANCE else null
}
