package cn.varsa.idea.pde.partial.manifest.completion

import cn.varsa.idea.pde.partial.core.manifest.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.util.*

object BundleSymbolicNameCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.editor.project ?: return
    result.addAllElements(
      BundleManifestIndex.getAllManifest(project).values.asSequence()
                            .filter { it.fragmentHost == null }.mapNotNull { it.bundleSymbolicName?.key }.distinct()
                            .sorted().map { LookupElementBuilder.create(it).withCaseSensitivity(false) }.toList())
  }
}