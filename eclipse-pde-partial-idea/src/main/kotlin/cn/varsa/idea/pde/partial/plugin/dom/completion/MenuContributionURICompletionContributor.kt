package cn.varsa.idea.pde.partial.plugin.dom.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.editor.*
import com.intellij.patterns.*
import com.intellij.psi.*
import com.intellij.psi.xml.*
import com.intellij.util.*

class MenuContributionURICompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
        .withSuperParent(2, PlatformPatterns.psiElement(PsiElement::class.java).withName("locationURI"))
        .withSuperParent(3, PlatformPatterns.psiElement(XmlTag::class.java).withName("menuContribution")),
      SchemeProvider()
    )
  }
}

private val addResultWithCaret = { value: String, result: CompletionResultSet, caret: String ->
  result.addElement(LookupElementBuilder.create(value).withCaseSensitivity(false).withInsertHandler { context, _ ->
    context.setAddCompletionChar(false)
    EditorModificationUtil.insertStringAtCaret(context.editor, caret)
    context.commitDocument()
  })
}

class SchemeProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
  ) {
    arrayOf("menu", "popup", "toolbar").forEach { addResultWithCaret(it, result, ":") }
  }
}
