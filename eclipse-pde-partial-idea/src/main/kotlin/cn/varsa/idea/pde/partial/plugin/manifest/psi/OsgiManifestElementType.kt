package cn.varsa.idea.pde.partial.plugin.manifest.psi

import com.intellij.extapi.psi.*
import com.intellij.lang.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.util.*
import org.jetbrains.lang.manifest.psi.*

abstract class OsgiManifestElementType(name: String) : ManifestElementType(name) {
  companion object {
    val attribute = object : OsgiManifestElementType("ATTRIBUTE") {
      override fun createPsi(node: ASTNode): PsiElement = Attribute(node)
    }

    val directive = object : OsgiManifestElementType("DIRECTIVE") {
      override fun createPsi(node: ASTNode): PsiElement = Directive(node)
    }

    val clause = object : OsgiManifestElementType("CLAUSE") {
      override fun createPsi(node: ASTNode): PsiElement = Clause(node)
    }
  }
}

abstract class AbstractAssignmentExpression(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {
  override fun getName(): String = getNameElement()?.unwrappedText ?: "<unnamed>"
  override fun setName(name: String): PsiElement = throw IncorrectOperationException()

  fun getNameElement(): HeaderValuePart? = PsiTreeUtil.getChildOfType(this, HeaderValuePart::class.java)
  fun getValueElement(): HeaderValuePart? =
    getNameElement()?.let { PsiTreeUtil.getNextSiblingOfType(it, HeaderValuePart::class.java) }

  fun getValue(): String = getValueElement()?.unwrappedText ?: ""
}

class Attribute(node: ASTNode) : AbstractAssignmentExpression(node) {
  override fun toString(): String = "Attribute: $name"
}

class Directive(node: ASTNode) : AbstractAssignmentExpression(node) {
  override fun toString(): String = "Directive: $name"
}

class Clause(node: ASTNode) : ASTWrapperPsiElement(node), HeaderValue {
  override fun getUnwrappedText(): String = text.replace("(?s)\\s*\n\\s*".toRegex(), "").trim()
  override fun toString(): String = "Clause"

  fun getValue(): HeaderValuePart? = findChildByClass(HeaderValuePart::class.java)
  fun getAttributes(): List<Attribute> = PsiTreeUtil.getChildrenOfTypeAsList(this, Attribute::class.java)
  fun getDirectives(): List<Directive> = PsiTreeUtil.getChildrenOfTypeAsList(this, Directive::class.java)
}
