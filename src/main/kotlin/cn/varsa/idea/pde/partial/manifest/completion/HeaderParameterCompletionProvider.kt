package cn.varsa.idea.pde.partial.manifest.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.util.ProcessingContext

class HeaderParameterCompletionProvider(private val names: Collection<String>) :
  CompletionProvider<CompletionParameters>() {
  constructor(vararg names: String) : this(names.toList())

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) = result.addAllElements(names.map {
    LookupElementBuilder.create(it.substringBeforeLast(':'))
      .withCaseSensitivity(false)
      .withInsertHandler(if (it.endsWith(':')) DIRECTIVE_HANDLER else ATTRIBUTE_HANDLER)
  })

  companion object {
    private val ATTRIBUTE_HANDLER = InsertHandler<LookupElement> { context, _ ->
      context.setAddCompletionChar(false)
      EditorModificationUtil.insertStringAtCaret(context.editor, "=")
      context.commitDocument()
    }

    private val DIRECTIVE_HANDLER = InsertHandler<LookupElement> { context, _ ->
      context.setAddCompletionChar(false)
      EditorModificationUtil.insertStringAtCaret(context.editor, ":=")
      context.commitDocument()
    }
  }
}