package cn.varsa.idea.pde.partial.manifest.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.lang.manifest.psi.ManifestElementType

sealed class BundleManifestElementType(name: String) : ManifestElementType(name) {
  object ATTRIBUTE : BundleManifestElementType("ATTRIBUTE") {
    override fun createPsi(node: ASTNode): PsiElement = ManifestHeaderPart.Attribute(node)
  }

  object DIRECTIVE : BundleManifestElementType("DIRECTIVE") {
    override fun createPsi(node: ASTNode): PsiElement = ManifestHeaderPart.Directive(node)
  }

  object CLAUSE : BundleManifestElementType("CLAUSE") {
    override fun createPsi(node: ASTNode): PsiElement = ManifestHeaderPart.Clause(node)
  }
}