package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.support.*
import com.intellij.openapi.vfs.*

val VirtualFile.fileProtocolUrl: String get() = presentableUrl.toFile().protocolUrl

val VirtualFile.protocolUrl: String
    get() = if (extension?.toLowerCase() == "jar" && fileSystem != JarFileSystem.getInstance()) {
        VirtualFileManager.constructUrl(StandardFileSystems.JAR_PROTOCOL, path)
            .let { if (it.contains(JarFileSystem.JAR_SEPARATOR)) it else "$it${JarFileSystem.JAR_SEPARATOR}" }
    } else {
        url
    }
