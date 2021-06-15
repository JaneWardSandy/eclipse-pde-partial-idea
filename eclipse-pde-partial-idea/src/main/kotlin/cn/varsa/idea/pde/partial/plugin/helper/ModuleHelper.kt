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
import org.osgi.framework.Constants.*
import java.io.*

object ModuleHelper {
    private val logger = thisLogger()

    // Compile output path
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
        ReadAction.compute<BundleManifest?, Exception> { cacheService.getManifest(module) } ?: return

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
        val cacheService = BundleManifestCacheService.getInstance(module.project)
        val manifest =
            ReadAction.compute<BundleManifest?, Exception> { cacheService.getManifest(module) } ?: return false

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

    // Global library
    fun resetLibrary(project: Project) {
        if (project.allPDEModules().isEmpty()) return
        val cacheService = BundleManifestCacheService.getInstance(project)
        val symbolicName2Bundle = BundleManagementService.getInstance(project).bundles

        val moduleNames = project.allPDEModules()
            .mapNotNull { ReadAction.compute<BundleManifest, Exception> { cacheService.getManifest(it) } }
            .mapNotNull { it.bundleSymbolicName?.key }.toSet()

        LibraryTablesRegistrar.getInstance().getLibraryTable(project).modifiableModel.also { model ->
            model.libraries.filter { library ->
                library.name?.let { name ->
                    name.substringAfter(ProjectLibraryNamePrefix, name)
                        .let { it != name && (moduleNames.contains(it) || !symbolicName2Bundle.containsKey(it)) }
                } == true
            }.forEach { model.removeLibrary(it) }

            ApplicationManager.getApplication().invokeAndWait { WriteAction.run<Exception> { model.commit() } }
        }

        symbolicName2Bundle.filterKeys { !moduleNames.contains(it) }.values.also { bundles ->
            val model = LibraryTablesRegistrar.getInstance().getLibraryTable(project).modifiableModel
            val map = hashMapOf<BundleDefinition, Library>()

            ApplicationManager.getApplication().invokeAndWait {
                bundles.forEach { bundle ->
                    val libraryName = "$ProjectLibraryNamePrefix${bundle.bundleSymbolicName}"
                    map[bundle] = model.getLibraryByName(libraryName)
                        ?: WriteAction.compute<Library, Exception> { model.createLibrary(libraryName) }
                }
            }

            map.map { (bundle, library) ->
                val libraryModel = library.modifiableModel

                libraryModel.getUrls(OrderRootType.CLASSES)
                    .forEach { libraryModel.removeRoot(it, OrderRootType.CLASSES) }
                libraryModel.getUrls(OrderRootType.SOURCES)
                    .forEach { libraryModel.removeRoot(it, OrderRootType.SOURCES) }

                bundle.delegateClassPathFile.map { it.protocolUrl }
                    .forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }
                bundle.sourceBundle?.delegateClassPathFile?.map { it.protocolUrl }
                    ?.forEach { libraryModel.addRoot(it, OrderRootType.SOURCES) }

                libraryModel
            }.also { list ->
                ApplicationManager.getApplication()
                    .invokeAndWait { WriteAction.run<Exception> { list.forEach { it.commit() } } }
            }

            ApplicationManager.getApplication().invokeAndWait { WriteAction.run<Exception> { model.commit() } }

            project.allPDEModules().forEach { module ->
                ModuleRootModificationUtil.updateModel(module) { model ->
                    map.forEach { (bundle, library) ->
                        (model.findLibraryOrderEntry(library) ?: model.addLibraryEntry(library)).apply {
                            scope = bundle.dependencyScope
                            isExported = false
                        }
                    }
                }
            }
        }
    }

    // Module library
    fun resetLibrary(module: Module) {
        PDEFacet.getInstance(module) ?: return

        BundleManifestCacheService.getInstance(module.project)
            .let { ReadAction.compute<BundleManifest?, Exception> { it.getManifest(module) } }?.also { manifest ->
                manifest.bundleClassPath?.keys?.filterNot { it == "." }?.flatMap { binaryName ->
                    ModuleRootManager.getInstance(module).contentRoots.mapNotNull { it.findFileByRelativePath(binaryName) }
                }?.map { it.protocolUrl }?.distinct()?.toSet()?.also {
                    setLibrary(module, DependencyScope.COMPILE, true, it)
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

        ModuleRootModificationUtil.updateModel(module) { model ->
            model.orderEntries.filter { it.presentableName.startsWith(ProjectLibraryNamePrefix) || it is ModuleOrderEntry }
                .forEach { model.removeOrderEntry(it) }
        }

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

        ApplicationManager.getApplication().invokeAndWait {
            val moduleLibraryTable = model.moduleLibraryTable.modifiableModel

            val library =
                moduleLibraryTable.getLibraryByName(ModuleLibraryName) ?: WriteAction.compute<Library, Exception> {
                    moduleLibraryTable.createLibrary(ModuleLibraryName)
                }

            model.findLibraryOrderEntry(library)?.apply {
                scope = dependencyScope
                isExported = exported

                val orderEntriesList = model.orderEntries.toMutableList()
                orderEntriesList.remove(this)

                val lastIndex = orderEntriesList.indexOfLast { it is JdkOrderEntry || it is ModuleSourceOrderEntry }
                orderEntriesList.add(lastIndex + 1, this)

                model.rearrangeOrderEntries(orderEntriesList.toTypedArray())
            }

            val libraryModel = library.modifiableModel

            libraryModel.getUrls(OrderRootType.CLASSES).forEach { libraryModel.removeRoot(it, OrderRootType.CLASSES) }
            libraryModel.getUrls(OrderRootType.SOURCES).forEach { libraryModel.removeRoot(it, OrderRootType.SOURCES) }

            classesRootUrls.forEach { libraryModel.addRoot(it, OrderRootType.CLASSES) }
            sourceRootUrls.forEach { libraryModel.addRoot(it, OrderRootType.SOURCES) }

            WriteAction.run<Exception> {
                libraryModel.commit()
                moduleLibraryTable.commit()
            }
        }

        val cacheService = BundleManifestCacheService.getInstance(model.project)
        model.module.also { module ->
            ApplicationManager.getApplication().invokeAndWait {
                model.project.allPDEModules().filter { it != module }.filter {
                    module.isBundleRequiredOrFromReExport(ReadAction.compute<String, Exception> {
                        cacheService.getManifest(it)?.bundleSymbolicName?.key ?: it.name
                    })
                }.forEach { model.addModuleOrderEntry(it) }
            }
        }

        val managementService = BundleManagementService.getInstance(model.project)
        LibraryTablesRegistrar.getInstance().getLibraryTable(model.project).libraries.filter {
            it.name?.startsWith(ProjectLibraryNamePrefix) == true
        }.forEach { dependencyLib ->
            managementService.bundles[dependencyLib.name?.substringAfter(ProjectLibraryNamePrefix)]?.dependencyScope?.also {
                model.addLibraryEntry(dependencyLib).apply {
                    scope = it
                    isExported = false
                }
            }
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
