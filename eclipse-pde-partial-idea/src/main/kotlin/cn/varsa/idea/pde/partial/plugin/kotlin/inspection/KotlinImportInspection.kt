package cn.varsa.idea.pde.partial.plugin.kotlin.inspection

import cn.varsa.idea.pde.partial.common.*
import cn.varsa.idea.pde.partial.common.support.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.inspection.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInspection.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*
import org.osgi.framework.Constants.*

class KotlinImportInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is KtFile) return null

        val project = file.project

        val projectFileIndex = ProjectFileIndex.getInstance(project)
        if (file.virtualFile?.let(projectFileIndex::isInLibrary) == true) return null

        val module = file.module ?: return null
        PDEFacet.getInstance(module) ?: return null
        module.isBundleRequiredOrFromReExport(KotlinBundleSymbolName).ifTrue { return null }

        val managementService = BundleManagementService.getInstance(project)
        val fixes = managementService.bundles[KotlinBundleSymbolName]?.manifest?.bundleVersion?.toString()
            ?.let { arrayOf(KotlinRequireBundleFix(it)) } ?: emptyArray()

        return arrayOf(
            manager.createProblemDescriptor(
                file,
                message("inspection.hint.bundleNotRequired", KotlinBundleSymbolName),
                isOnTheFly,
                fixes + KotlinRequireBundleFix(),
                ProblemHighlightType.ERROR
            )
        )
    }
}

class KotlinRequireBundleFix(version: String? = null) : AbstractOsgiQuickFix() {
    private val headerValue =
        "$KotlinBundleSymbolName${if (version.isNullOrBlank()) "" else ";$BUNDLE_VERSION_ATTRIBUTE=\"$version\""}"

    override fun getName(): String = message("inspection.fix.addBundleToRequired", headerValue)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        getVerifiedManifestFile(descriptor.psiElement)?.also {
            writeCompute { PsiHelper.appendToHeader(it, REQUIRE_BUNDLE, headerValue) }
        }
    }
}
