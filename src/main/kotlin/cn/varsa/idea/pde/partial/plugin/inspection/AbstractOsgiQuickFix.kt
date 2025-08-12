package cn.varsa.idea.pde.partial.plugin.inspection

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInspection.*
import com.intellij.openapi.module.*
import com.intellij.psi.*
import com.intellij.refactoring.util.*
import org.jetbrains.lang.manifest.psi.*

abstract class AbstractOsgiQuickFix : LocalQuickFix {
  override fun getFamilyName(): String = message("inspection.group")
  override fun startInWriteAction(): Boolean = false

  internal fun getVerifiedManifestFile(element: PsiElement): ManifestFile? =
    ModuleUtilCore.findModuleForPsiElement(element)?.getManifestFile()
      ?.let { element.manager.findFile(it) as? ManifestFile }?.takeIf { CommonRefactoringUtil.checkReadOnlyStatus(it) }
}
