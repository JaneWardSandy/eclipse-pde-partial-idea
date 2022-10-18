package cn.varsa.idea.pde.partial.plugin.helper

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import com.intellij.packaging.artifacts.*
import com.intellij.packaging.elements.*
import com.intellij.packaging.impl.artifacts.*
import org.osgi.framework.Constants.*
import java.io.*

object ModuleHelper {
  private val logger = thisLogger()

  // Compile output path
  fun resetCompileOutputPath(module: Module) {
    val facet = PDEFacet.getInstance(module) ?: return
    setCompileOutputPath(
      module,
      "${module.getModuleDir()}/${facet.configuration.compilerClassesOutput}",
      "${module.getModuleDir()}/${facet.configuration.compilerTestClassesOutput}"
    )
  }

  fun setCompileOutputPath(module: Module, compilerOutputPath: String, compilerOutputPathForTest: String) {
    PDEFacet.getInstance(module) ?: return
    ModuleRootModificationUtil.modifyModel(module) {
      setCompileOutputPath(
        it, compilerOutputPath, compilerOutputPathForTest
      )
    }
  }

  private fun setCompileOutputPath(
    model: ModifiableRootModel, compilerOutputPath: String, compilerOutputPathForTest: String
  ): Boolean {
    PDEFacet.getInstance(model.module) ?: return false
    val extension = model.getModuleExtension(CompilerModuleExtension::class.java) ?: return false

    extension.isExcludeOutput = true
    extension.inheritCompilerOutputPath(false)
    extension.setCompilerOutputPath(
      VirtualFileManager.constructUrl(StandardFileSystems.FILE_PROTOCOL, compilerOutputPath)
    )
    extension.setCompilerOutputPathForTests(
      VirtualFileManager.constructUrl(StandardFileSystems.FILE_PROTOCOL, compilerOutputPathForTest)
    )

    return true
  }

  // Artifact
  fun resetCompileArtifact(module: Module) {
    val facet = PDEFacet.getInstance(module) ?: return
    setCompileArtifact(module, addBinary = facet.configuration.binaryOutput)
  }

  fun setCompileArtifact(
    module: Module, addBinary: Set<String> = emptySet(), removeBinary: Set<String> = emptySet()
  ) {
    PDEFacet.getInstance(module) ?: return
    if (addBinary.isEmpty() && removeBinary.isEmpty()) return

    val cacheService = BundleManifestCacheService.getInstance(module.project)
    readCompute { cacheService.getManifest(module) } ?: return

    val model = readCompute { ArtifactManager.getInstance(module.project).createModifiableModel() }
    try {
      if (setCompileArtifact(module, model, addBinary, removeBinary)) {
        applicationInvokeAndWait { if (!module.project.isDisposed) writeCompute(model::commit) }
      }
    } finally {
      model.dispose()
    }
  }

  private fun setCompileArtifact(
    module: Module,
    model: ModifiableArtifactModel,
    addBinary: Set<String> = emptySet(),
    removeBinary: Set<String> = emptySet()
  ): Boolean {
    val cacheService = BundleManifestCacheService.getInstance(module.project)
    val manifest = readCompute { cacheService.getManifest(module) } ?: return false

    val factory = PackagingElementFactory.getInstance()

    val artifactName = "$ArtifactPrefix${module.name}"

    val artifact = model.findArtifact(artifactName)?.let {
      if (it.artifactType !is JarArtifactType) {
        model.removeArtifact(it)
        null
      } else {
        it
      }
    }?.let { model.getOrCreateModifiableArtifact(it) } ?: model.addArtifact(
      artifactName, JarArtifactType.getInstance()
    )

    artifact.outputPath?.toFile()?.takeIf { it.name != Artifacts && it.parentFile.name == Artifacts }?.also {
      artifact.outputPath = it.parentFile.canonicalPath
      logger.info("Change artifact output path to: ${artifact.outputPath}")
    }

    artifact.rootElement.apply {
      rename("${manifest.bundleSymbolicName?.key}_${manifest.bundleVersion}.jar")
      addOrFindChild(factory.createModuleOutput(module))

      removeBinary.mapNotNull { findCompositeChild(it) }.also { removeChildren(it) }
      addBinary.map { File(module.getModuleDir(), it) }.filter { it.exists() }
        .filter { findCompositeChild(it.name) == null }.map {
          if (it.isFile) {
            factory.createFileCopy(it.canonicalPath, null)
          } else {
            factory.createDirectoryCopyWithParentDirectories(it.canonicalPath, it.name)
          }
        }.forEach { addOrFindChild(it) }
    }

    logger.info("Eclipse PDE Partial Bundle artifact changed: $artifactName, addBinary: $addBinary, removeBinary: $removeBinary")
    return true
  }

  fun setupManifestFile(module: Module) {
    PDEFacet.getInstance(module) ?: return
    val directory = module.rootManager.contentRoots.firstOrNull() ?: return

    val metaInfDir = directory.findChild(MetaInf)?.also { it.refresh(false, false) }?.takeIf { it.isValid }
      ?: writeComputeAndWait { directory.createChildDirectory(null, MetaInf) }
    val manifestMfFile = metaInfDir.findChild(ManifestMf)?.takeIf { it.isValid }

    if (manifestMfFile != null) return
    writeRunAndWait {
      VfsUtil.saveText(
        metaInfDir.createChildData(null, ManifestMf), """
                Manifest-Version: 1.0
                $BUNDLE_MANIFESTVERSION: 2
                $BUNDLE_NAME: ${module.name.substringAfterLast('.').uppercase()}
                $BUNDLE_SYMBOLICNAME: ${module.name};$SINGLETON_DIRECTIVE:=true
                $BUNDLE_VERSION: 1.0.0
                $BUNDLE_ACTIVATOR: ${module.name.lowercase()}.Activator
                $BUNDLE_VENDOR: Varsa Studio
                $REQUIRE_BUNDLE: org.eclipse.ui,
                 org.eclipse.core.runtime
                $BUNDLE_REQUIREDEXECUTIONENVIRONMENT: ${JavaVersions.OpenJDK7.ee}
                $BUNDLE_ACTIVATIONPOLICY: $ACTIVATION_LAZY
                $BUNDLE_CLASSPATH: .

        """.trimIndent()
      )
    }

    if (directory.findChild(PluginsXml).let { it == null || !it.isValid }) {
      writeRunAndWait {
        VfsUtil.saveText(
          directory.createChildData(null, PluginsXml), """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <?eclipse version="3.4"?>
                    <plugin>

                        <extension point="org.eclipse.ui.commands">
                        </extension>

                        <extension point="org.eclipse.ui.menus">
                            <menuContribution locationURI="menu:org.eclipse.ui.main.menu?after=additions">
                            </menuContribution>
                        </extension>

                    </plugin>

            """.trimIndent()
        )
      }
    }
  }
}
