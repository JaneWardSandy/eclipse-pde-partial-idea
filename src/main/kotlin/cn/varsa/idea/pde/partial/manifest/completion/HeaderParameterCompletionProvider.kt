package cn.varsa.idea.pde.partial.manifest.completion

import cn.varsa.idea.pde.partial.common.Constants.Eclipse.X_FRIENDS_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.USES_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VERSION_ATTRIBUTE
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.editor.*
import com.intellij.util.*

class HeaderParameterCompletionProvider(private val names: Collection<String>) :
  CompletionProvider<CompletionParameters>() {
  constructor(vararg names: String) : this(names.toList())

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) = result.addAllElements(names.map {
    LookupElementBuilder.create(it.substringBeforeLast(':')).withCaseSensitivity(false).withInsertHandler(
      when {
        it.endsWith(':') -> when {
          it.removeSuffix(":") in setOf(USES_DIRECTIVE, X_FRIENDS_DIRECTIVE) -> QUOTED_DIRECTIVE_HANDLER
          else -> DIRECTIVE_HANDLER
        }

        it in setOf(VERSION_ATTRIBUTE, BUNDLE_VERSION_ATTRIBUTE) -> QUOTED_ATTRIBUTE_HANDLER
        else -> ATTRIBUTE_HANDLER
      }
    )
  })

  companion object {
    private val ATTRIBUTE_HANDLER = InsertHandler<LookupElement> { context, _ ->
      context.setAddCompletionChar(false)
      EditorModificationUtil.insertStringAtCaret(context.editor, "=")
      context.commitDocument()
    }

    private val QUOTED_ATTRIBUTE_HANDLER = InsertHandler<LookupElement> { context, _ ->
      context.setAddCompletionChar(false)
      EditorModificationUtil.insertStringAtCaret(context.editor, "=\"\"", false, true, 2)
      context.commitDocument()
    }

    private val DIRECTIVE_HANDLER = InsertHandler<LookupElement> { context, _ ->
      context.setAddCompletionChar(false)
      EditorModificationUtil.insertStringAtCaret(context.editor, ":=")
      context.commitDocument()
    }

    private val QUOTED_DIRECTIVE_HANDLER = InsertHandler<LookupElement> { context, _ ->
      context.setAddCompletionChar(false)
      EditorModificationUtil.insertStringAtCaret(context.editor, ":=\"\"", false, true, 3)
      context.commitDocument()
    }
  }
}