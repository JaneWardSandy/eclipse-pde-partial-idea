package cn.varsa.idea.pde.partial.manifest.completion

import cn.varsa.idea.pde.partial.manifest.psi.AssignmentExpression
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import org.jetbrains.lang.manifest.psi.*

abstract class BundleManifestCompletionContributor : CompletionContributor() {
  fun valuePart(name: String): PsiElementPattern.Capture<PsiElement> = PlatformPatterns
    .psiElement(ManifestTokenType.HEADER_VALUE_PART)
    .withSuperParent(2, PlatformPatterns.psiElement(Header::class.java).withName(name))

  fun clause(name: String): PsiElementPattern.Capture<PsiElement> = PlatformPatterns
    .psiElement(ManifestTokenType.HEADER_VALUE_PART)
    .withSuperParent(3, PlatformPatterns.psiElement(Header::class.java).withName(name))

  fun header(name: String): PsiElementPattern.Capture<PsiElement> = PlatformPatterns
    .psiElement(ManifestTokenType.HEADER_VALUE_PART)
    .afterLeaf(";")
    .withSuperParent(3, PlatformPatterns.psiElement(Header::class.java).withName(name))

  fun attribute(name: String): PsiElementPattern.Capture<PsiElement> = PlatformPatterns
    .psiElement(ManifestTokenType.HEADER_VALUE_PART)
    .withSuperParent(2, PlatformPatterns.psiElement(AssignmentExpression.Attribute::class.java).withName(name))

  fun directive(name: String): PsiElementPattern.Capture<PsiElement> = PlatformPatterns
    .psiElement(ManifestTokenType.HEADER_VALUE_PART)
    .withSuperParent(2, PlatformPatterns.psiElement(AssignmentExpression.Directive::class.java).withName(name))
}