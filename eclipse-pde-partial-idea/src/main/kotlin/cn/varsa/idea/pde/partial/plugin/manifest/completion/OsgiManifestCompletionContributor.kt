package cn.varsa.idea.pde.partial.plugin.manifest.completion

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.manifest.psi.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.editor.*
import com.intellij.patterns.*
import com.intellij.util.*
import org.jetbrains.lang.manifest.psi.*
import org.osgi.framework.Constants.*

class OsgiManifestCompletionContributor : CompletionContributor() {
    init {
        val clause = { name: String ->
            PlatformPatterns.psiElement(ManifestTokenType.HEADER_VALUE_PART)
                .withSuperParent(3, PlatformPatterns.psiElement(Header::class.java).withName(name))
        }
        val header = { name: String ->
            PlatformPatterns.psiElement(ManifestTokenType.HEADER_VALUE_PART).afterLeaf(";")
                .withSuperParent(3, PlatformPatterns.psiElement(Header::class.java).withName(name))
        }
        val directive = { name: String ->
            PlatformPatterns.psiElement(ManifestTokenType.HEADER_VALUE_PART)
                .withSuperParent(2, PlatformPatterns.psiElement(Directive::class.java).withName(name))
        }


        // Clause Value
        extend(
            CompletionType.BASIC,
            clause(BUNDLE_REQUIREDEXECUTIONENVIRONMENT),
            ValueProvider(*JavaVersions.values().map { it.ee }.toTypedArray())
        )
        extend(CompletionType.BASIC, clause(REQUIRE_BUNDLE), BundleNameProvider())
        extend(CompletionType.BASIC, clause(FRAGMENT_HOST), BundleNameProvider())
        extend(CompletionType.BASIC, clause("Eclipse-ExtensibleAPI"), ValueProvider(true.toString(), false.toString()))

        // Directive Key
        extend(
            CompletionType.BASIC,
            header(BUNDLE_SYMBOLICNAME),
            HeaderParametersProvider("$SINGLETON_DIRECTIVE:", "$FRAGMENT_ATTACHMENT_DIRECTIVE:")
        )
        extend(
            CompletionType.BASIC,
            header(REQUIRE_BUNDLE),
            HeaderParametersProvider(BUNDLE_VERSION_ATTRIBUTE, "$VISIBILITY_DIRECTIVE:", "$RESOLUTION_DIRECTIVE:")
        )
        extend(
            CompletionType.BASIC,
            header(EXPORT_PACKAGE),
            HeaderParametersProvider(VERSION_ATTRIBUTE, "$USES_DIRECTIVE:")
        )
        extend(
            CompletionType.BASIC,
            header(IMPORT_PACKAGE),
            HeaderParametersProvider(VERSION_ATTRIBUTE, BUNDLE_SYMBOLICNAME_ATTRIBUTE, "$RESOLUTION_DIRECTIVE:")
        )
        extend(CompletionType.BASIC, header(ACTIVATION_LAZY), ValueProvider(ACTIVATION_LAZY))
        extend(CompletionType.BASIC, header(FRAGMENT_HOST), HeaderParametersProvider(BUNDLE_VERSION_ATTRIBUTE))

        // Directive Value
        extend(CompletionType.BASIC, directive(SINGLETON_DIRECTIVE), ValueProvider(true.toString(), false.toString()))
        extend(
            CompletionType.BASIC,
            directive(FRAGMENT_ATTACHMENT_DIRECTIVE),
            ValueProvider(FRAGMENT_ATTACHMENT_ALWAYS, FRAGMENT_ATTACHMENT_RESOLVETIME, FRAGMENT_ATTACHMENT_NEVER)
        )
        extend(
            CompletionType.BASIC,
            directive(VISIBILITY_DIRECTIVE),
            ValueProvider(VISIBILITY_PRIVATE, VISIBILITY_REEXPORT)
        )
        extend(
            CompletionType.BASIC,
            directive(RESOLUTION_DIRECTIVE),
            ValueProvider(RESOLUTION_MANDATORY, RESOLUTION_OPTIONAL)
        )
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
        names.forEach {
            result.addElement(
                LookupElementBuilder.create(it.substringBeforeLast(':')).withCaseSensitivity(false)
                    .withInsertHandler(if (it.endsWith(':')) directiveHandler else attributeHandler)
            )
        }
    }
}

class ValueProvider(private vararg val values: String) : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
    ) {
        values.forEach {
            result.addElement(LookupElementBuilder.create(it).withCaseSensitivity(false))
        }
    }
}

class BundleNameProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
    ) {
        parameters.editor.project?.let { project ->
            project.allPDEModulesSymbolicName(parameters.originalFile.module) + BundleManagementService.getInstance(
                project
            ).getBundles().map { it.bundleSymbolicName }
        }?.distinct()?.sorted()?.forEach {
            result.addElement(LookupElementBuilder.create(it).withCaseSensitivity(false))
        }
    }
}
