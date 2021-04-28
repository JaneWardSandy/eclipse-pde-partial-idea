package cn.varsa.idea.pde.partial.plugin.helper

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.vfs.*
import com.intellij.packaging.artifacts.*
import com.intellij.packaging.elements.*
import com.intellij.packaging.impl.artifacts.*
import com.jetbrains.rd.util.*
import org.jetbrains.kotlin.idea.util.projectStructure.*
import org.osgi.framework.*
import org.osgi.framework.Constants.*
import java.io.*

object ModuleHelper {
    private val logger = thisLogger()

    fun resetCompileOutputPath(module: Module) {
        val facet = PDEFacet.getInstance(module) ?: return
        setCompileOutputPath(module, "${module.getModuleDir()}/${facet.configuration.compilerOutputDirectory}")
    }

    fun setCompileOutputPath(module: Module, baseDirectory: String) {
        PDEFacet.getInstance(module) ?: return
        ModuleRootModificationUtil.modifyModel(module) { setCompileOutputPath(it, baseDirectory) }
    }

    private fun setCompileOutputPath(model: ModifiableRootModel, baseDirectory: String): Boolean {
        PDEFacet.getInstance(model.module) ?: return false
        val extension = model.getModuleExtension(CompilerModuleExtension::class.java) ?: return false

        extension.isExcludeOutput = true
        extension.inheritCompilerOutputPath(false)
        extension.setCompilerOutputPath(
            VirtualFileManager.constructUrl(
                StandardFileSystems.FILE_PROTOCOL, "$baseDirectory/${CompilerModuleExtension.PRODUCTION}"
            )
        )
        extension.setCompilerOutputPathForTests(
            VirtualFileManager.constructUrl(
                StandardFileSystems.FILE_PROTOCOL, "$baseDirectory/${CompilerModuleExtension.TEST}"
            )
        )

        logger.info("Update compiler output for ${model.module.name} to $baseDirectory")
        return true
    }

    fun resetCompileArtifact(module: Module) {
        val facet = PDEFacet.getInstance(module) ?: return
        setCompileArtifact(module, addBinary = facet.configuration.binaryOutput)
    }

    fun setCompileArtifact(
        module: Module, addBinary: Set<String> = emptySet(), removeBinary: Set<String> = emptySet()
    ) {
        PDEFacet.getInstance(module) ?: return
        if (addBinary.isEmpty() && removeBinary.isEmpty()) return
        BundleManifestCacheService.getInstance(module.project).getManifest(module) ?: return

        val model = ReadAction.compute<ModifiableArtifactModel, Exception> {
            ArtifactManager.getInstance(module.project).createModifiableModel()
        }
        try {
            if (setCompileArtifact(module, model, addBinary, removeBinary)) {
                ApplicationManager.getApplication().invokeAndWait {
                    if (!module.project.isDisposed) WriteAction.run<Exception>(model::commit)
                }
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
        val manifest = BundleManifestCacheService.getInstance(module.project).getManifest(module) ?: return false
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

    fun resetLibrary(project: Project) {
        if (project.allModules().filter { it.isLoaded }.none { PDEFacet.getInstance(it) != null }) return
        val cacheService = BundleManifestCacheService.getInstance(project)

        val dependencyToUrls = DependencyScope.values().associateWith { Pair(HashSet<String>(), HashSet<String>()) }
        val addedBundleToVersion =
            project.allModules().asSequence().filter { it.isLoaded }.mapNotNull { cacheService.getManifest(it) }
                .associateBy({ it.bundleSymbolicName?.key }) { it.bundleVersion?.toString() }
                .filter { it.key != null && it.value != null }.map { it.key!! to it.value!! }
                .let { hashMapOf(*it.toTypedArray()) }

        TargetDefinitionService.getInstance(project).locations.forEach { location ->
            dependencyToUrls.filterKeys { it.displayName == location.dependency }.firstOrNull()?.value?.run {
                location.bundles.filter { it.exists() }
                    .mapNotNull { LocalFileSystem.getInstance().findFileByIoFile(it) }.forEach { file ->
                        cacheService.getManifest(file)?.also {
                            val symbolicName = it.bundleSymbolicName?.key ?: ""
                            val version = (it.bundleVersion ?: Version.emptyVersion).toString()

                            val sourceBundle = it.eclipseSourceBundle
                            if (sourceBundle != null) {
                                if (addedBundleToVersion[sourceBundle.key] == it.bundleVersion?.toString()) {
                                    second += file.protocolUrl
                                }
                            } else {
                                addedBundleToVersion.computeIfAbsent(symbolicName) {
                                    first += file.protocolUrl
                                    version
                                }
                            }
                        }
                    }
            }
        }

        dependencyToUrls.forEach { (scope, urls) ->
            setLibrary(project, "$ProjectLibraryNamePrefix${scope.displayName}", scope, urls.first, urls.second)
        }
    }

    private fun setLibrary(
        project: Project,
        libraryName: String,
        dependencyScope: DependencyScope,
        classesRootUrls: Set<String> = emptySet(),
        sourceRootUrls: Set<String> = emptySet()
    ) {
        if (project.allModules().filter { it.isLoaded }.none { PDEFacet.getInstance(it) != null }) return

        val library = LibraryTablesRegistrar.getInstance().getLibraryTable(project).let {
            it.getLibraryByName(libraryName)
                ?: WriteAction.compute<Library, Exception> { it.createLibrary(libraryName) }
        }
        val libraryModel = library.modifiableModel

        OrderRootType.getAllTypes()
            .forEach { type -> libraryModel.getUrls(type).forEach { libraryModel.removeRoot(it, type) } }

        classesRootUrls.forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }
        sourceRootUrls.forEach { libraryModel.addRoot(it, OrderRootType.SOURCES) }

        ApplicationManager.getApplication().invokeAndWait { WriteAction.run<Exception> { libraryModel.commit() } }

        project.allModules().filter { it.isLoaded }.filter { PDEFacet.getInstance(it) != null }.forEach { module ->
            ModuleRootModificationUtil.updateModel(module) { model ->
                (model.findLibraryOrderEntry(library) ?: model.addLibraryEntry(library)).apply {
                    scope = dependencyScope
                    isExported = false
                }
            }
        }
    }

    fun resetLibrary(module: Module) {
        PDEFacet.getInstance(module) ?: return

        BundleManifestCacheService.getInstance(module.project).getManifest(module)?.also { manifest ->
            manifest.bundleClassPath?.keys?.filterNot { it == "." }?.flatMap { binaryName ->
                ModuleRootManager.getInstance(module).contentRoots.mapNotNull { it.findFileByRelativePath(binaryName) }
            }?.map { it.protocolUrl }?.distinct()?.toSet()?.also {
                setLibrary(module, DependencyScope.COMPILE, false, it)
            }
        }
    }

    private fun setLibrary(
        module: Module,
        dependencyScope: DependencyScope,
        exported: Boolean = false,
        classesRootUrls: Set<String> = emptySet(),
        sourceRootUrls: Set<String> = emptySet()
    ) {
        PDEFacet.getInstance(module) ?: return

        ModuleRootModificationUtil.updateModel(module) {
            setLibrary(it, dependencyScope, exported, classesRootUrls, sourceRootUrls)
        }
    }

    private fun setLibrary(
        model: ModifiableRootModel,
        dependencyScope: DependencyScope,
        exported: Boolean = false,
        classesRootUrls: Set<String> = emptySet(),
        sourceRootUrls: Set<String> = emptySet()
    ) {
        PDEFacet.getInstance(model.module) ?: return

        model.orderEntries.filterNot {
            it is ModuleSourceOrderEntry || it is JdkOrderEntry || it.presentableName.startsWith("KotlinJavaRuntime")
        }.forEach { model.removeOrderEntry(it) }

        val library = model.moduleLibraryTable.let {
            it.getLibraryByName(ModuleLibraryName) ?: WriteAction.compute<Library, Exception> {
                it.createLibrary(ModuleLibraryName)
            }
        }
        val libraryModel = library.modifiableModel

        model.findLibraryOrderEntry(library)?.apply {
            scope = dependencyScope
            isExported = exported
        }

        OrderRootType.getAllTypes()
            .forEach { type -> libraryModel.getUrls(type).forEach { libraryModel.removeRoot(it, type) } }

        classesRootUrls.forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }
        sourceRootUrls.forEach { libraryModel.addRoot(it, OrderRootType.SOURCES) }

        ApplicationManager.getApplication().invokeAndWait { WriteAction.run<Exception> { libraryModel.commit() } }

        val cacheService = BundleManifestCacheService.getInstance(model.project)
        cacheService.getManifest(model.module)?.also { manifest ->
            model.project.allModules().filter { it.isLoaded }.filter { it != model.module }.filter {
                val symbolicName = cacheService.getManifest(it)?.bundleSymbolicName?.key ?: it.name
                manifest.isBundleRequired(symbolicName)// fixme || it.isBundleRequiredFromReExport(name)
            }.forEach { model.addModuleOrderEntry(it) }
        }
    }

    fun setupManifestFile(module: Module) {
        PDEFacet.getInstance(module) ?: return
        val directory = module.rootManager.contentRoots.firstOrNull() ?: return

        val metaInfDir = directory.findChild(MetaInf)?.also { it.refresh(false, false) }?.takeIf { it.isValid }
            ?: directory.createChildDirectory(null, MetaInf)
        val manifestMfFile = metaInfDir.findChild(ManifestMf)?.takeIf { it.isValid }

        if (manifestMfFile != null) return
        VfsUtil.saveText(
            metaInfDir.createChildData(null, ManifestMf), """
                Manifest-Version: 1.0
                $BUNDLE_MANIFESTVERSION: 2
                $BUNDLE_NAME: ${module.name.substringAfterLast('.').toUpperCase()}
                $BUNDLE_SYMBOLICNAME: ${module.name};$SINGLETON_DIRECTIVE:=true
                $BUNDLE_VERSION: 1.0.0
                $BUNDLE_ACTIVATOR: ${module.name.toLowerCase()}.Activator
                $BUNDLE_VENDOR: Varsa Studio
                $REQUIRE_BUNDLE: org.eclipse.ui,
                 org.eclipse.core.runtime
                $BUNDLE_REQUIREDEXECUTIONENVIRONMENT: ${JavaVersions.OpenJDK7.ee}
                $BUNDLE_ACTIVATIONPOLICY: $ACTIVATION_LAZY
                $BUNDLE_CLASSPATH: .

        """.trimIndent()
        )

        if (directory.findChild(PluginsXml).let { it == null || !it.isValid }) {
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
