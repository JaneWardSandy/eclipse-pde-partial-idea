package cn.varsa.idea.pde.partial.plugin.domain

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import com.intellij.util.*
import java.io.*

data class BundleDefinition(
    val root: VirtualFile, val file: File, val project: Project, val dependencyScope: DependencyScope
) {
    val manifest: BundleManifest? get() = BundleManifestCacheService.getInstance(project).getManifest(root)

    val bundleSymbolicName: String get() = manifest?.bundleSymbolicName?.key ?: file.nameWithoutExtension
    private val classPaths: Map<String, VirtualFile>
        get() = mapOf("" to root) + (manifest?.bundleClassPath?.keys?.filterNot { it == "." }
            ?.map { it to root.findFileByRelativePath(it) }?.filterNot { it.second == null }
            ?.associate { it.first to it.second!! } ?: emptyMap())

    val delegateClassPathFile: Map<String, VirtualFile>
        get() = classPaths.mapValues {
            val originFile = it.value

            val rootEntry = JarFileSystem.getInstance().getRootByEntry(originFile)
            if (rootEntry != null && rootEntry != originFile) {
                val name =
                    "${PathUtilRt.suggestFileName("${root.name}${originFile.presentableUrl.substringAfter(rootEntry.presentableUrl)}")}.${originFile.extension}"

                val projectFile = LocalFileSystem.getInstance().findFileByPath(project.presentableUrl!!)!!
                val outFile = readCompute { projectFile.findChild("out") } ?: writeComputeAndWait {
                    projectFile.createChildDirectory(this, "out")
                }
                val libFile = readCompute { outFile.findChild("tmp_lib") } ?: writeComputeAndWait {
                    outFile.createChildDirectory(this, "tmp_lib")
                }
                val virtualFile = readCompute { libFile.findChild(name) } ?: writeComputeAndWait {
                    libFile.createChildData(this, name)
                }

                if (virtualFile.modificationStamp != rootEntry.modificationStamp || virtualFile.timeStamp != rootEntry.timeStamp) {
                    writeComputeAndWait {
                        virtualFile.getOutputStream(virtualFile, rootEntry.modificationStamp, rootEntry.timeStamp)
                            .use { ous -> originFile.inputStream.use { ins -> ins.copyTo(ous) } }
                    }
                }

                virtualFile
            } else {
                originFile
            }
        }

    var sourceBundle: BundleDefinition? = null
}
