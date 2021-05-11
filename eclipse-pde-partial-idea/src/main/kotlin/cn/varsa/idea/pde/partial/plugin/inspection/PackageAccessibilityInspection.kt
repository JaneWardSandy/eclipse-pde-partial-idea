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
                is KtNamedDeclaration -> checkAccessibility(dependency, facet)?.also {
                    list.add(manager.createProblemDescriptor(place, it.message, isOnTheFly, it.fixes, it.type))
                }
            }
        }, DependencyVisitorFactory.VisitorOptions.SKIP_IMPORTS)

        return list.takeIf { it.isNotEmpty() }?.toTypedArray()
    }

    companion object {
        fun checkAccessibility(namedDeclaration: KtNamedDeclaration, facet: PDEFacet): Problem? {
            val targetFile = namedDeclaration.containingFile
            if (targetFile !is KtFile) return null

            val packageName = targetFile.packageFqName.asString()
            if (packageName.isBlank() || packageName.startsWith(Kotlin)) return null

            val qualifiedName = namedDeclaration.fqName?.asString() ?: return null

            if (facet.module == ModuleUtilCore.findModuleForPsiElement(namedDeclaration)) return null

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
            if (packageName.isBlank() || packageName.startsWith(Java)) return null

            val qualifiedName = targetClass.qualifiedName ?: return null

            if (facet.module == ModuleUtilCore.findModuleForPsiElement(targetClass)) return null

            return checkAccessibility(targetFile, packageName, qualifiedName, facet.module)
        }

        private fun checkAccessibility(
            item: PsiFileSystemItem, packageName: String, qualifiedName: String, requesterModule: Module
        ): Problem? {
            // In bundleClassPath
            val library = requesterModule.findLibrary { it.name == ModuleLibraryName }
            if (library?.let { LibraryUtil.isClassAvailableInLibrary(it, qualifiedName) } == true) return null

            val cacheService = BundleManifestCacheService.getInstance(requesterModule.project)

            val exporter = cacheService.getManifest(item)
            val exporterSymbolic = exporter?.bundleSymbolicName
            if (exporter == null || exporterSymbolic == null) {
                return Problem.weak(message("inspection.hint.nonBundle", packageName))
            }

            val exporterSymbolicName = exporter.fragmentHost?.key ?: exporterSymbolic.key

            val exporterExportedPackage =
                exporter.getExportedPackage(packageName) ?: cacheService.getManifestByBundleSymbolName(
                    exporterSymbolicName
                )?.getExportedPackage(packageName) ?: return Problem.error(
                    message("inspection.hint.packageNoExport", packageName)
                )

            val importer = cacheService.getManifest(requesterModule)
            if (importer != null) {
                if (importer.isPackageImported(packageName)) return null
                if (importer.isBundleRequired(exporterSymbolicName)) return null
                if (requesterModule.isBundleRequiredOrFromReExport(exporterSymbolicName)) return null

                // FIXME: 2021/5/7 Class in bundle-classpath and it was exported
                // like /Eclipse.app/Contents/Eclipse/plugins/org.apache.ant_1.10.9.v20201106-1946/lib/ant-antlr.jar
                // ant bundle can resolve, but class inside lib cannot be read
                if (requesterModule.isExportedPackageFromRequiredBundle(packageName)) return null
            }

            val requiredFixes = cacheService.getVersionByBundleSymbolName(exporterSymbolicName)
                ?.let { arrayOf(RequireBundleFix(exporterSymbolicName, it.toString())) } ?: emptyArray()

            return Problem.error(
                message("inspection.hint.packageAccessibility", packageName, exporterSymbolicName),
                ImportPackageFix(exporterExportedPackage),
                *requiredFixes + RequireBundleFix(exporterSymbolicName)
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
