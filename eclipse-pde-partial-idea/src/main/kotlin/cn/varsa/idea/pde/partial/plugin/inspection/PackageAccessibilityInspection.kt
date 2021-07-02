package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import cn.varsa.idea.pde.partial.plugin.support.getModuleDir
import com.intellij.codeInsight.daemon.impl.analysis.*
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.*
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.packageDependencies.*
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.refactoring.fqName.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.idea.util.projectStructure.*
import org.jetbrains.kotlin.nj2k.postProcessing.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.osgi.framework.Constants.*
import java.lang.annotation.*

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
        ): Problem? {
            // In bundleClassPath
            val library = requesterModule.findLibrary { it.name == ModuleLibraryName }
            if (library?.let { LibraryUtil.isClassAvailableInLibrary(it, qualifiedName) } == true) return null

            val project = requesterModule.project
            val cacheService = BundleManifestCacheService.getInstance(project)
            val managementService = BundleManagementService.getInstance(project)
            val index = ProjectFileIndex.getInstance(project)

            // In bundle class path?
            val jarFile = index.getClassRootForFile(item.virtualFile)
            val containerBundle =
                managementService.jarPathInnerBundle[jarFile?.presentableUrl]?.manifest ?: project.allPDEModules()
                    .filterNot { requesterModule == it }.firstOrNull { module ->
                        jarFile?.presentableUrl == module.getModuleDir() || cacheService.getManifest(module)?.bundleClassPath?.keys?.filterNot { it == "." }
                            ?.mapNotNull { module.getModuleDir().toFile(it).canonicalPath }
                            ?.any { jarFile?.presentableUrl == it } == true
                    }?.let { cacheService.getManifest(it) }


            val exporter = containerBundle ?: cacheService.getManifest(item)
            val exporterSymbolic = exporter?.bundleSymbolicName
            if (exporter == null || exporterSymbolic == null) {
                return Problem.weak(message("inspection.hint.nonBundle", packageName))
            }

            val exporterSymbolicName = exporter.fragmentHost?.key ?: exporterSymbolic.key

            val exporterExportedPackage = exporter.getExportedPackage(packageName)
                ?: managementService.bundles[exporterSymbolicName]?.manifest?.getExportedPackage(packageName)
                ?: return Problem.error(message("inspection.hint.packageNoExport", packageName, exporterSymbolicName))

            val importer = cacheService.getManifest(requesterModule)
            if (importer != null) {
                if (importer.isPackageImported(packageName)) return null
                if (importer.isBundleRequired(exporterSymbolicName)) return null
                if (requesterModule.isBundleRequiredOrFromReExport(exporterSymbolicName)) return null
            }

            val requiredFixes = managementService.bundles[exporterSymbolicName]?.manifest?.bundleVersion?.toString()
                ?.let { arrayOf(AccessibilityFix.requireBundleFix(exporterSymbolicName, it)) } ?: emptyArray()

            return Problem.error(
                message("inspection.hint.packageAccessibility", packageName, exporterSymbolicName),
                AccessibilityFix.importPackageFix(exporterExportedPackage),
                *requiredFixes + AccessibilityFix.requireBundleFix(exporterSymbolicName)
            )
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
