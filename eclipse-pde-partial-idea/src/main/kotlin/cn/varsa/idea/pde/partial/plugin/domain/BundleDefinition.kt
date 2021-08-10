package cn.varsa.idea.pde.partial.plugin.domain

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import com.intellij.util.*
import org.osgi.framework.*
import java.io.*

data class BundleDefinition(
    val root: VirtualFile,
    val file: File,
    val location: TargetLocationDefinition,
    val project: Project,
    val dependencyScope: DependencyScope
) {
    val manifest: BundleManifest? get() = BundleManifestCacheService.getInstance(project).getManifest(root)

    val bundleSymbolicName: String get() = manifest?.bundleSymbolicName?.key ?: file.nameWithoutExtension
    val bundleVersion: Version get() = manifest?.bundleVersion ?: Version.emptyVersion

    private val classPaths: Map<String, VirtualFile>
        get() = mapOf("" to root) + (manifest?.bundleClassPath?.keys?.filterNot { it == "." }
            ?.map { it to root.findFileByRelativePath(it) }?.filterNot { it.second == null || it.second!!.isDirectory }
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

                if ((virtualFile.modificationStamp != originFile.modificationStamp || virtualFile.timeStamp != originFile.timeStamp) && virtualFile.length != originFile.length) {
                    writeComputeAndWait {
                        virtualFile.getOutputStream(virtualFile, originFile.modificationStamp, originFile.timeStamp)
                            .use { ous -> originFile.inputStream.use { ins -> ins.copyTo(ous) } }
                    }
                }

                virtualFile
            } else {
                originFile
            }
        }

    var sourceBundle: BundleDefinition? = null

    val canonicalName: String get() = "$bundleSymbolicName-$bundleVersion"
    override fun toString(): String = canonicalName
}

val Pair<String, Version>.asCanonicalName: String get() = "$first-$second"
