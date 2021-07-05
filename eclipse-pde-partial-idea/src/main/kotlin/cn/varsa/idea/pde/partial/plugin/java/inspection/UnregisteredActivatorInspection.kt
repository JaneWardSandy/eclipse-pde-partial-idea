package cn.varsa.idea.pde.partial.plugin.java.inspection

import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.facet.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.inspection.*
import com.intellij.codeInspection.*
import com.intellij.openapi.command.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.psi.*
import org.osgi.framework.*

class UnregisteredActivatorInspection : AbstractOsgiVisitor() {
    override fun buildVisitor(facet: PDEFacet, holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        if (holder.file !is PsiClassOwner) PsiElementVisitor.EMPTY_VISITOR
        else object : JavaElementVisitor() {
            override fun visitFile(file: PsiFile) {
                val projectFileIndex = ProjectFileIndex.getInstance(file.project)
                if (file.virtualFile?.let(projectFileIndex::isInLibrary) == true) return

                val cacheService = BundleManifestCacheService.getInstance(facet.module.project)

                (file as? PsiClassOwner)?.classes?.filter { PsiHelper.isActivator(it) && it.qualifiedName != null }
                    ?.filter { cacheService.getManifest(facet.module)?.bundleActivator != it.qualifiedName }?.forEach {
                        val identifier = unwrap(it.nameIdentifier)
                        if (isValidElement(identifier)) {
                            holder.registerProblem(
                                identifier!!,
                                message("inspection.hint.unregisteredActivator"),
                                RegisterInManifestQuickfix(it.qualifiedName!!)
                            )
                        }
                    }
            }
        }
}

class RegisterInManifestQuickfix(private val activatorClass: String) : AbstractOsgiQuickFix() {
    override fun getName(): String = message("inspection.fix.registerActivator", activatorClass)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        getVerifiedManifestFile(descriptor.endElement)?.also {
            WriteCommandAction.writeCommandAction(project, it).run<Exception> {
                PsiHelper.setHeader(it, Constants.BUNDLE_ACTIVATOR, activatorClass)
            }
        }
    }
}
