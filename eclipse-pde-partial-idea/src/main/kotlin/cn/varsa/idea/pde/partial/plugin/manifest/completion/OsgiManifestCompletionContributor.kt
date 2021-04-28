package cn.varsa.idea.pde.partial.plugin.manifest.completion

import cn.varsa.idea.pde.partial.plugin.manifest.psi.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.editor.*
import com.intellij.patterns.*
import com.intellij.util.*
import org.jetbrains.lang.manifest.psi.*
import org.osgi.framework.Constants.*

class OsgiManifestCompletionContributor : CompletionContributor() {
    init {
        val header = { name: String ->
            PlatformPatterns.psiElement(ManifestTokenType.HEADER_VALUE_PART).afterLeaf(";")
                .withSuperParent(3, PlatformPatterns.psiElement(Header::class.java).withName(name))
        }
        val directive = { name: String ->
            PlatformPatterns.psiElement(ManifestTokenType.HEADER_VALUE_PART)
                .withSuperParent(2, PlatformPatterns.psiElement(Directive::class.java).withName(name))
        }

        extend(
            CompletionType.BASIC,
            header(EXPORT_PACKAGE),
            HeaderParametersProvider(VERSION_ATTRIBUTE, "$USES_DIRECTIVE:")
        )

        extend(
            CompletionType.BASIC,
            header(IMPORT_PACKAGE),
            HeaderParametersProvider(VERSION_ATTRIBUTE, "$RESOLUTION_DIRECTIVE:")
        )

        extend(
            CompletionType.BASIC,
            directive(RESOLUTION_DIRECTIVE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    arrayOf(RESOLUTION_MANDATORY, RESOLUTION_OPTIONAL).forEach { name ->
                        result.addElement(LookupElementBuilder.create(name).withCaseSensitivity(false))
                    }
                }
            })
    }
}

class HeaderParametersProvider(private vararg val names: String) : CompletionProvider<CompletionParameters>() {
    private val attributeHandler = InsertHandler<LookupElement> { context, _ ->
        context.setAddCompletionChar(false)
        EditorModificationUtil.insertStringAtCaret(context.editor, "=")
        context.commitDocument()
    }

    private val directiveHandler = InsertHandler<LookupElement> { context, _ ->
        context.setAddCompletionChar(false)
        EditorModificationUtil.insertStringAtCaret(context.editor, ":=")
        context.commitDocument()
    }

    override fun addCompletions(
        parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
    ) {
        names.forEach { name ->
            result.addElement(
                LookupElementBuilder.create(name.substringBeforeLast(':')).withCaseSensitivity(false)
                    .withInsertHandler(if (name.endsWith(':')) directiveHandler else attributeHandler)
            )
        }
    }
}
