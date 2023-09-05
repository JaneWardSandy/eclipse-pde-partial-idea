package cn.varsa.idea.pde.partial.manifest.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.lang.manifest.psi.HeaderValue
import org.jetbrains.lang.manifest.psi.HeaderValuePart

sealed class AssignmentExpression(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {
  override fun getName(): String = getNameElement()?.unwrappedText ?: "<unnamed>"
  override fun setName(name: String): PsiElement = throw IncorrectOperationException()

  fun getNameElement(): HeaderValuePart? = PsiTreeUtil.getChildOfType(this, HeaderValuePart::class.java)
  fun getValueElement(): HeaderValuePart? =
    getNameElement()?.let { PsiTreeUtil.getNextSiblingOfType(it, HeaderValuePart::class.java) }

  fun getValue(): String = getValueElement()?.unwrappedText ?: ""


  class Attribute(node: ASTNode) : AssignmentExpression(node) {
    override fun toString(): String = "Attribute: $name"
  }

  class Directive(node: ASTNode) : AssignmentExpression(node) {
    override fun toString(): String = "Directive: $name"
  }

  class Clause(node: ASTNode) : ASTWrapperPsiElement(node), HeaderValue {
    override fun getUnwrappedText(): String = text.replace("(?s)\\s*\n\\s*".toRegex(), "").trim()
    override fun toString(): String = "Clause"

    fun getValue(): HeaderValuePart? = findChildByClass(HeaderValuePart::class.java)
    fun getAttributes(): List<Attribute> = PsiTreeUtil.getChildrenOfTypeAsList(this, Attribute::class.java)
    fun getDirectives(): List<Directive> = PsiTreeUtil.getChildrenOfTypeAsList(this, Directive::class.java)
  }
}