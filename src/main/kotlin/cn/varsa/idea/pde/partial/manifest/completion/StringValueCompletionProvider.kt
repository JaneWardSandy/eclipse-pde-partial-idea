package cn.varsa.idea.pde.partial.manifest.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.util.*

class StringValueCompletionProvider(private val values: Collection<String>) :
  CompletionProvider<CompletionParameters>() {
  constructor(vararg values: String) : this(values.toList())

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) = result.addAllElements(values.map { LookupElementBuilder.create(it).withCaseSensitivity(false) })
}