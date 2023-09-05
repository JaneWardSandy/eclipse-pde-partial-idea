package cn.varsa.idea.pde.partial.manifest.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.lang.manifest.psi.ManifestElementType

sealed class OsgiManifestElementType(name: String) : ManifestElementType(name) {
  object ATTRIBUTE : OsgiManifestElementType("ATTRIBUTE") {
    override fun createPsi(node: ASTNode): PsiElement = AssignmentExpression.Attribute(node)
  }

  object DIRECTIVE : OsgiManifestElementType("DIRECTIVE") {
    override fun createPsi(node: ASTNode): PsiElement = AssignmentExpression.Directive(node)
  }

  object CLAUSE : OsgiManifestElementType("CLAUSE") {
    override fun createPsi(node: ASTNode): PsiElement = AssignmentExpression.Clause(node)
  }
}