package cn.varsa.idea.pde.partial.inspection.java

import cn.varsa.idea.pde.partial.inspection.BasicInspector
import cn.varsa.idea.pde.partial.message.InspectionBundle
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

class JavaClassInDefaultPackageInspection : AbstractBaseJavaLocalInspectionTool(), BasicInspector {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    object : JavaElementVisitor() {
      override fun visitJavaFile(file: PsiJavaFile) {
        super.visitJavaFile(file)
        if (notContainPDEFacet(file)) return

        if (file.packageName.isBlank()) {
          holder.problem(file, InspectionBundle.message("inspection.hint.javaClassInDefaultPackage")).register()
        }
      }
    }
}