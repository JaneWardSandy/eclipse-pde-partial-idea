package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInsight.daemon.impl.analysis.*
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.*
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.application.*
import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.packageDependencies.*
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.idea.util.projectStructure.findLibrary
import org.jetbrains.kotlin.psi.*
import org.osgi.framework.Constants.*
import java.lang.annotation.*

class PackageAccessibilityInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is PsiClassOwner || ProjectRootsUtil.isInTestSource(file)) return null

        val projectFileIndex = ProjectFileIndex.getInstance(file.project)
        if (file.virtualFile?.let(projectFileIndex::isInLibrary) == true) return null

        val facet = file.module?.let { PDEFacet.getInstance(it) } ?: return null

        val list = mutableListOf<ProblemDescriptor>()
        DependenciesBuilder.analyzeFileDependencies(file, { place, dependency ->
            when (dependency) {
                is PsiClass -> checkAccessibility(dependency, facet)?.also {
                    list.add(manager.createProblemDescriptor(place, it.message, isOnTheFly, it.fixes, it.type))
                }
                is PsiMethod -> dependency.parent?.let { it as? PsiClass }?.let { checkAccessibility(it, facet) }
                    ?.also {
                        list.add(manager.createProblemDescriptor(place, it.message, isOnTheFly, it.fixes, it.type))
                    }
                is KtNamedFunction -> checkAccessibility(dependency, facet)?.also {
                    list.add(manager.createProblemDescriptor(place, it.message, isOnTheFly, it.fixes, it.type))
                }
            }
        }, DependencyVisitorFactory.VisitorOptions.SKIP_IMPORTS)

        return list.takeIf { it.isNotEmpty() }?.toTypedArray()
    }

    companion object {
        fun checkAccessibility(targetFunction: KtNamedFunction, facet: PDEFacet): Problem? {
            val targetFile = targetFunction.containingFile
            if (targetFile !is KtFile) return null

            val packageName = targetFile.packageFqName.asString()
            if (packageName.isBlank() || packageName.startsWith("kotlin")) return null

            val qualifiedName = targetFunction.fqName?.asString() ?: return null

            if (facet.module == ModuleUtilCore.findModuleForPsiElement(targetFunction)) return null

            return checkAccessibility(targetFile, packageName, qualifiedName, facet.module)
        }

        fun checkAccessibility(targetClass: PsiClass, facet: PDEFacet): Problem? {
            if (targetClass.isAnnotationType) {
                val policy = AnnotationsHighlightUtil.getRetentionPolicy(targetClass)
                if (policy == RetentionPolicy.CLASS || policy == RetentionPolicy.SOURCE) return null
            }

            val targetFile = targetClass.containingFile
            if (targetFile !is PsiClassOwner) return null

            val packageName = targetFile.packageName
            if (packageName.isBlank() || packageName.startsWith("java")) return null

            val qualifiedName = targetClass.qualifiedName ?: return null

            if (facet.module == ModuleUtilCore.findModuleForPsiElement(targetClass)) return null

            return checkAccessibility(targetFile, packageName, qualifiedName, facet.module)
        }

        private fun checkAccessibility(
            item: PsiFileSystemItem, packageName: String, qualifiedName: String, requesterModule: Module
        ): Problem? {
            val library = requesterModule.findLibrary { it.name == ModuleLibraryName }
            if (library?.let { LibraryUtil.isClassAvailableInLibrary(it, qualifiedName) } == true) return null

            val cacheService = BundleManifestCacheService.getInstance(requesterModule.project)

            val importer = cacheService.getManifest(requesterModule)
            if (importer?.getExportedPackage(packageName) != null) return null

            val exporter = cacheService.getManifest(item)
            val exporterSymbolicName = exporter?.bundleSymbolicName
            if (exporter == null || exporterSymbolicName == null) {
                return Problem.weak("The package '$packageName' is inside a non-bundle dependency")
            }
            val exporterBundleSymbolicName = exporter.fragmentHost?.key ?: exporterSymbolicName.key

            // FIXME: 2021/4/27
            val exporterExportedPackage = "" //exporter.getExportedPackage(packageName)
//                ?: cacheService.libSymbol2Manifest[exporterBundleSymbolicName]?.getExportedPackage(packageName)
//                ?: return Problem.error("The package '$packageName' is not exported by the bundle dependencies")

            if (importer != null) {
                if (importer.isPackageImported(packageName)) return null
                if (importer.isBundleRequired(exporterBundleSymbolicName)) return null

//                if (requesterModule.isBundleRequiredFromReExport(exporterBundleSymbolicName)) return null
//                if (requesterModule.isExportedPackageFromRequiredBundle(packageName)) return null
            }

            val requiredFixes =
            //cacheService.libSymbol2Versions[exporterBundleSymbolicName]?.takeIf { it.isNotEmpty() }
                /*?.map { RequireBundleFix(exporterBundleSymbolicName, it) }?.toTypedArray() ?: */
                arrayOf(RequireBundleFix(exporterBundleSymbolicName))

            return Problem.error(
                message("inspection.hint.packageAccessibility", packageName, exporterBundleSymbolicName),
                ImportPackageFix(exporterExportedPackage),
                *requiredFixes
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

class ImportPackageFix(private val importPackage: String) : AbstractOsgiQuickFix() {
    override fun getName(): String = message("inspection.fix.addPackageToImport", importPackage)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        getVerifiedManifestFile(descriptor.psiElement)?.also {
            WriteAction.run<Exception> {
                PsiHelper.appendToHeader(it, IMPORT_PACKAGE, importPackage)
            }
        }
    }
}

class RequireBundleFix(requireBundle: String, version: String? = null) : AbstractOsgiQuickFix() {
    private val headerValue =
        "$requireBundle${if (version.isNullOrBlank()) "" else ";$BUNDLE_VERSION_ATTRIBUTE=\"$version\""}"

    override fun getName(): String = message("inspection.fix.addBundleToRequired", headerValue)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        getVerifiedManifestFile(descriptor.psiElement)?.also {
            WriteAction.run<Exception> {
                PsiHelper.appendToHeader(it, REQUIRE_BUNDLE, headerValue)
            }
        }
    }
}
