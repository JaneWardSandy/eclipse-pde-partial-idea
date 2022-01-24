package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.*
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*

val VirtualFile.fileProtocolUrl: String get() = presentableUrl.toFile().protocolUrl

val VirtualFile.protocolUrl: String
    get() = if (extension?.lowercase() == "jar" && fileSystem != JarFileSystem.getInstance()) {
        VirtualFileManager.constructUrl(StandardFileSystems.JAR_PROTOCOL, path)
            .let { if (it.contains(JarFileSystem.JAR_SEPARATOR)) it else "$it${JarFileSystem.JAR_SEPARATOR}" }
    } else {
        url
    }

fun VirtualFile.validFile(): VirtualFile? = takeIf { isValid }

fun VirtualFile.validFileOrRequestResolve(project: Project, lazyMessage: (VirtualFile) -> String): VirtualFile? =
    validFile() ?: run {
        PdeNotifier.important("Virtual file invalid", lazyMessage(this))
            .addAction(object : NotificationAction(EclipsePDEPartialBundles.message("action.resolveRequest.text")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    e.project?.also { TargetDefinitionService.getInstance(it).backgroundResolve(it) }
                    notification.expire()
                }
            }).notify(project)
        null
    }

fun VirtualFile.isBelongJDK(project: Project): Boolean = isBelongJDK(ProjectFileIndex.getInstance(project))

fun VirtualFile.isBelongJDK(index: ProjectFileIndex): Boolean =
    index.getOrderEntriesForFile(this).let { it.size == 1 && it.first() is JdkOrderEntry }
