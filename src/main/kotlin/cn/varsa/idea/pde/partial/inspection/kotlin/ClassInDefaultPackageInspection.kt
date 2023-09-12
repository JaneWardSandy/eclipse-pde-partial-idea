package cn.varsa.idea.pde.partial.inspection.kotlin

import cn.varsa.idea.pde.partial.message.InspectionBundle
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*

class ClassInDefaultPackageInspection : AbstractKotlinInspection() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : KtVisitorVoid() {
    override fun visitKtFile(file: KtFile) {
      super.visitKtFile(file)

      if (file.packageFqName.isRoot) {
        holder.problem(file, InspectionBundle.message("inspection.hint.kotlinClassInDefaultPackage")).register()
      }
    }
  }
}