package cn.varsa.idea.pde.partial.inspection.kotlin

import cn.varsa.idea.pde.partial.inspection.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.*
import org.jetbrains.kotlin.psi.*

class ClassInDefaultPackageInspection : AbstractKotlinInspection(), BasicInspector {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : KtVisitorVoid() {
    override fun visitKtFile(file: KtFile) {
      super.visitKtFile(file)
      if (notContainPDEFacet(file)) return

      if (file.packageFqName.isRoot) {
        holder.problem(file, InspectionBundle.message("inspection.hint.kotlinClassInDefaultPackage")).register()
      }
    }
  }
}