package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.*
import com.intellij.ide.projectView.impl.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.packageDependencies.*
import com.intellij.psi.*
import org.osgi.framework.Constants.*

abstract class PackageAccessibilityInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is PsiClassOwner || ProjectRootsUtil.isInTestSource(file)) return null

        val projectFileIndex = ProjectFileIndex.getInstance(file.project)
        if (file.virtualFile?.let(projectFileIndex::isInLibrary) == true) return null

        val facet = file.module?.let { PDEFacet.getInstance(it) } ?: return null

        val problems = hashSetOf<ProblemDescriptor>()
        val addMessage: (Problem, PsiElement) -> Unit = { problem, place ->
            problems.add(
                manager.createProblemDescriptor(place, problem.message, isOnTheFly, problem.fixes, problem.type)
            )
        }

        DependenciesBuilder.analyzeFileDependencies(file, { place, dependency ->
            checkElement(place, dependency, facet, addMessage)
        }, DependencyVisitorFactory.VisitorOptions.SKIP_IMPORTS)

        return problems.takeIf { it.isNotEmpty() }?.toTypedArray()
    }

    abstract fun checkElement(
        place: PsiElement, dependency: PsiElement, facet: PDEFacet, occurProblem: (Problem, PsiElement) -> Unit
    )

    companion object {
        fun checkAccessibility(
            item: PsiFileSystemItem, packageName: String, qualifiedName: String, requesterModule: Module
        ): List<Problem> {
            // In bundleClassPath
            val library = requesterModule.findLibrary { it.name == ModuleLibraryName }
            if (library?.let { LibraryUtil.isClassAvailableInLibrary(it, qualifiedName) } == true) return emptyList()

            val project = requesterModule.project
            val cacheService = BundleManifestCacheService.getInstance(project)
            val managementService = BundleManagementService.getInstance(project)
            val index = ProjectFileIndex.getInstance(project)

            // In bundle class path?
            val containers = arrayListOf<BundleManifest>()

            cacheService.getManifest(item)?.also { containers += it }
            val jarFile = index.getClassRootForFile(item.virtualFile)
            jarFile?.presentableUrl?.let { managementService.getBundleByInnerJarPath(it)?.manifest }
                ?.also { containers += it }
            containers += project.allPDEModules().filterNot { requesterModule == it }.filter { module ->
                jarFile?.presentableUrl == module.getModuleDir() || cacheService.getManifest(module)?.bundleClassPath?.keys?.filterNot { it == "." }
                    ?.mapNotNull { module.getModuleDir().toFile(it).canonicalPath }
                    ?.any { jarFile?.presentableUrl == it } == true
            }.mapNotNull { cacheService.getManifest(it) }


            val problems = arrayListOf<Problem>()
            if (containers.isEmpty()) problems += Problem.weak(message("inspection.hint.nonBundle", packageName))
            containers.forEach { exporter ->
                val exporterSymbolic = exporter.bundleSymbolicName
                if (exporterSymbolic == null) {
                    problems += Problem.weak(message("inspection.hint.nonBundle", packageName))
                    return@forEach
                }

                val exporterBSN = exporter.fragmentHost?.key ?: exporterSymbolic.key
                val exporterVersions = hashSetOf(exporter.bundleVersion)

                val exporterExportedPackageVersions = exporter.exportedPackageAndVersion(packageName).values.toHashSet()
                managementService.getBundlesByBSN(exporterBSN)?.mapValues { it.value.manifest }
                    ?.mapValues { it.value?.exportedPackageAndVersion(packageName)?.values }?.also { map ->
                        exporterVersions += map.filterValues { it?.isNotEmpty() == true }.keys
                        exporterExportedPackageVersions += map.values.filterNotNull().flatten()
                    }
                if (exporterExportedPackageVersions.isEmpty()) {
                    problems += Problem.error(
                        message(
                            "inspection.hint.packageNoExport", packageName, exporterBSN
                        )
                    )
                    return@forEach
                }

                val importer = cacheService.getManifest(requesterModule)
                if (importer != null) {
                    if (importer.isPackageImported(packageName, exporterExportedPackageVersions)) return emptyList()
                    if (importer.isBundleRequired(exporterBSN, exporterVersions)) return emptyList()
                    if (requesterModule.isBundleRequiredOrFromReExport(
                            exporterBSN, exporterVersions
                        )
                    ) return emptyList()
                }

                val requiredFixes =
                    exporterVersions.map { it.toString() }.map { AccessibilityFix.requireBundleFix(exporterBSN, it) }
                        .toTypedArray()

                problems += Problem.error(
                    message("inspection.hint.packageAccessibility", packageName, exporterBSN),
                    AccessibilityFix.importPackageFix(packageName),
                    *requiredFixes + AccessibilityFix.requireBundleFix(exporterBSN)
                )
            }

            return problems
        }
    }
}

class Problem(val type: ProblemHighlightType, val message: @InspectionMessage String, vararg val fixes: LocalQuickFix) {
    companion object {
        fun weak(message: @InspectionMessage String, vararg fixes: LocalQuickFix): Problem =
            Problem(ProblemHighlightType.WEAK_WARNING, message, *fixes)

        fun error(message: @InspectionMessage String, vararg fixes: LocalQuickFix): Problem =
            Problem(ProblemHighlightType.ERROR, message, *fixes)
    }
}

class AccessibilityFix private constructor(
    private val displayName: String, private val headerName: String, private val headerValue: String
) : AbstractOsgiQuickFix() {
    override fun getName(): String = displayName
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        getVerifiedManifestFile(descriptor.psiElement)?.also {
            writeCompute { PsiHelper.appendToHeader(it, headerName, headerValue) }
        }
    }

    companion object Factory {
        fun importPackageFix(importPackage: String): AccessibilityFix =
            AccessibilityFix(message("inspection.fix.addPackageToImport", importPackage), IMPORT_PACKAGE, importPackage)

        fun requireBundleFix(requireBundle: String, version: String? = null): AccessibilityFix {
            val headerValue =
                "$requireBundle${if (version.isNullOrBlank()) "" else ";$BUNDLE_VERSION_ATTRIBUTE=\"$version\""}"

            return AccessibilityFix(
                message("inspection.fix.addBundleToRequired", headerValue), REQUIRE_BUNDLE, headerValue
            )
        }
    }
}
