package cn.varsa.idea.pde.partial.plugin.manifest.lang

import cn.varsa.idea.pde.partial.common.domain.*
import cn.varsa.idea.pde.partial.plugin.cache.*
import cn.varsa.idea.pde.partial.plugin.config.*
import cn.varsa.idea.pde.partial.plugin.helper.*
import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.manifest.psi.*
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.lang.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.*
import com.intellij.psi.tree.*
import org.jetbrains.lang.manifest.header.*
import org.jetbrains.lang.manifest.header.impl.*
import org.jetbrains.lang.manifest.parser.*
import org.jetbrains.lang.manifest.psi.*
import org.osgi.framework.*
import org.osgi.framework.Constants.*
import org.osgi.framework.Version

private fun AnnotationHolder.createError(message: String, range: TextRange) =
    newAnnotation(HighlightSeverity.ERROR, message).range(range).create()

object OsgiHeaderParser : StandardHeaderParser() {
    private val clauseEndTokens =
        TokenSet.orSet(ManifestParser.HEADER_END_TOKENS, TokenSet.create(ManifestTokenType.COMMA))
    private val subClauseEndTokens = TokenSet.orSet(clauseEndTokens, TokenSet.create(ManifestTokenType.SEMICOLON))

    override fun parse(builder: PsiBuilder) {
        while (!builder.eof()) {
            if (!parseClause(builder)) break

            val tokenType = builder.tokenType
            if (ManifestParser.HEADER_END_TOKENS.contains(tokenType)) break
            if (ManifestTokenType.COMMA == tokenType) builder.advanceLexer()
        }
    }

    private fun parseClause(builder: PsiBuilder): Boolean {
        val clause = builder.mark()
        var result = true

        while (!builder.eof()) {
            if (!parseSubClause(builder, false)) {
                result = false
                break
            }

            val tokenType = builder.tokenType
            if (clauseEndTokens.contains(tokenType)) break
            if (ManifestTokenType.SEMICOLON == tokenType) builder.advanceLexer()
        }

        clause.done(OsgiManifestElementType.clause)
        return result
    }

    private fun parseSubClause(builder: PsiBuilder, assigment: Boolean = true): Boolean {
        val mark = builder.mark()
        var result = true

        while (!builder.eof()) {
            val tokenType = builder.tokenType
            if (subClauseEndTokens.contains(tokenType)) break
            if (ManifestTokenType.QUOTE == tokenType) {
                parseQuotedString(builder)
            } else if (!assigment && ManifestTokenType.EQUALS == tokenType) {
                mark.done(ManifestElementType.HEADER_VALUE_PART)
                return parseAttribute(builder, mark.precede())
            } else if (!assigment && ManifestTokenType.COLON == tokenType) {
                mark.done(ManifestElementType.HEADER_VALUE_PART)
                return parseDirective(builder, mark.precede())
            } else {
                val lastTokenType = builder.tokenType
                builder.advanceLexer()

                if (ManifestTokenType.NEWLINE == lastTokenType && ManifestTokenType.SIGNIFICANT_SPACE != builder.tokenType) {
                    result = false
                    break
                }
            }
        }

        mark.done(ManifestElementType.HEADER_VALUE_PART)
        return result
    }

    private fun parseQuotedString(builder: PsiBuilder) {
        do {
            builder.advanceLexer()
        } while (!builder.eof() && !ManifestParser.HEADER_END_TOKENS.contains(builder.tokenType) && !PsiBuilderUtil.expect(
                builder, ManifestTokenType.QUOTE
            )
        )
    }

    private fun parseAttribute(builder: PsiBuilder, mark: PsiBuilder.Marker): Boolean {
        builder.advanceLexer()
        val result = parseSubClause(builder)
        mark.done(OsgiManifestElementType.attribute)
        return result
    }

    private fun parseDirective(builder: PsiBuilder, mark: PsiBuilder.Marker): Boolean {
        builder.advanceLexer()

        if (PsiBuilderUtil.expect(builder, ManifestTokenType.NEWLINE)) {
            PsiBuilderUtil.expect(builder, ManifestTokenType.SIGNIFICANT_SPACE)
        }
        PsiBuilderUtil.expect(builder, ManifestTokenType.EQUALS)

        val result = parseSubClause(builder)
        mark.done(OsgiManifestElementType.directive)
        return result
    }
}

object BasePackageParser : HeaderParser by OsgiHeaderParser {

    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        var annotated = false

        header.headerValues.mapNotNull { it as? Clause }.mapNotNull(Clause::getValue).forEach {
            val packageName = it.unwrappedText.substringBeforeLast(".*")
            if (packageName.isBlank()) {
                holder.createError(message("manifest.lang.invalidReference"), it.highlightingRange)
                annotated = true
            } else if (PsiHelper.resolvePackage(header, packageName).isEmpty()) {
                holder.createError(message("manifest.lang.cannotResolvePackage", packageName), it.highlightingRange)
                annotated = true
            }
        }

        return annotated
    }

    override fun getConvertedValue(header: Header): Any? =
        header.headerValues.takeIf(MutableList<HeaderValue>::isNotEmpty)?.mapNotNull { it as? Clause }
            ?.mapNotNull(Clause::getValue)?.map { it.unwrappedText }

    override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> =
        if (headerValuePart.parent is Clause) getPackageReferences(headerValuePart) else PsiReference.EMPTY_ARRAY

    internal fun getPackageReferences(element: PsiElement): Array<PsiReference> {
        var packageName = element.text
        if (packageName.isBlank()) return PsiReference.EMPTY_ARRAY

        var offset = 0
        if (packageName.startsWith('!')) {
            packageName = packageName.substringAfter('!')
            offset = 1
        }
        if (packageName.endsWith('?')) packageName = packageName.substringBeforeLast('?')

        return object : PackageReferenceSet(packageName, element, offset) {
            override fun resolvePackageName(
                context: PsiPackage?,
                packageName: String,
            ): MutableCollection<PsiPackage> {
                val unwrappedPackageName = packageName.replace("\\s".toRegex(), "")
                return context?.subPackages?.filter { unwrappedPackageName == it.name }?.toMutableList()
                    ?: mutableListOf()
            }
        }.references.toTypedArray()
    }
}

object BundleManifestVersionParser : StandardHeaderParser() {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        val headerValue = header.headerValue
        if (headerValue is HeaderValuePart && headerValue.unwrappedText.toIntOrNull() != 2) {
            holder.createError(message("manifest.lang.manifestVersion"), headerValue.highlightingRange)
            return true
        }
        return false
    }

    override fun getConvertedValue(header: Header): Any? = header.headerValue?.unwrappedText?.toIntOrNull()
}

object NotBlankValueParser : StandardHeaderParser() {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        val headerValue = header.headerValue
        if (headerValue is HeaderValuePart && headerValue.unwrappedText.isBlank()) {
            holder.createError(message("manifest.lang.invalidBlank"), headerValue.highlightingRange)
            return true
        }
        return false
    }
}

object BundleSymbolicNameParser : HeaderParser by OsgiHeaderParser {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        var annotated = false

        header.headerValues.mapNotNull { it as? Clause }.forEach { clause ->
            val valuePart = clause.getValue()
            if (valuePart == null || valuePart.unwrappedText.isBlank()) {
                holder.createError(
                    message("manifest.lang.invalidBlank"), valuePart?.highlightingRange ?: clause.textRange
                )
                annotated = true
            }

            clause.getDirectives().run {
                if (any { it.name != SINGLETON_DIRECTIVE }) {
                    holder.createError(
                        message("manifest.lang.specifyDirectivesOnly", SINGLETON_DIRECTIVE), clause.textRange
                    )
                    annotated = true
                }

                firstOrNull { it.name == SINGLETON_DIRECTIVE }?.getValueElement()
                    ?.takeUnless { arrayOf("true", "false").contains(it.unwrappedText) }?.let {
                        holder.createError(
                            message("manifest.lang.shouldBe", SINGLETON_DIRECTIVE, "true or false"),
                            it.highlightingRange
                        )
                        annotated = true
                    }
            }
        }

        return annotated
    }
}

object BundleVersionParser : StandardHeaderParser() {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        val headerValue = header.headerValue
        if (headerValue is HeaderValuePart) {
            try {
                Version.parseVersion(headerValue.unwrappedText)
            } catch (e: IllegalArgumentException) {
                holder.createError(e.message ?: "", headerValue.highlightingRange)
                return true
            }
        }
        return false
    }

    override fun getConvertedValue(header: Header): Any? = try {
        header.headerValue?.unwrappedText?.let(Version::parseVersion)
    } catch (e: IllegalArgumentException) {
        null
    }
}

object BundleActivatorParser : ClassReferenceParser() {
    override fun checkClass(valuePart: HeaderValuePart, aClass: PsiClass, holder: AnnotationHolder): Boolean {
        val activatorClass = PsiHelper.getActivatorClass(valuePart.project)
        if (activatorClass != null && !aClass.isInheritor(activatorClass, true)) {
            holder.createError(message("manifest.lang.invalidActivator"), valuePart.highlightingRange)
            return true
        }
        return false
    }
}

object RequireBundleParser : HeaderParser by OsgiHeaderParser {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        var annotated = false

        header.headerValues.mapNotNull { it as? Clause }.forEach { clause ->
            val text = clause.getValue()?.unwrappedText
            if (text.isNullOrBlank()) {
                holder.createError(message("manifest.lang.invalidBlank"), clause.textRange)
                annotated = true
            } else {
                val project = header.project
                val cacheService = BundleManifestCacheService.getInstance(project)

                if (header.module?.let { cacheService.getManifest(it) }?.bundleSymbolicName?.key == text) {
                    holder.createError(message("manifest.lang.invalidValue", text), clause.textRange)
                    annotated = true
                } else if (BundleManagementService.getInstance(project).bundles[text] == null && project.allPDEModules()
                        .mapNotNull { cacheService.getManifest(it) }.mapNotNull { it.bundleSymbolicName?.key }
                        .none { it == text }
                ) {
                    holder.createError(message("manifest.lang.invalidReference"), clause.textRange)
                    annotated = true
                }
            }
        }

        return annotated
    }

    override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> =
        if (headerValuePart.parent is Clause) arrayOf(BundleReference(headerValuePart)) else PsiReference.EMPTY_ARRAY
}

object RequiredExecutionEnvironmentParser : HeaderParser by OsgiHeaderParser {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        var annotated = false

        header.headerValues.mapNotNull { it as? Clause }.forEach {
            val text = it.getValue()?.unwrappedText
            if (text.isNullOrBlank() || JavaVersions.getJava(text) == JavaVersions.UNKNOWN) {
                holder.createError(message("manifest.lang.invalidJavaVersion"), it.textRange)
                annotated = true
            }
        }

        return annotated
    }
}

object BundleActivationPolicyParser : HeaderParser by OsgiHeaderParser {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        var annotated = false

        header.headerValues.mapNotNull { it as? Clause }.forEach { clause ->
            if (clause.getValue()?.unwrappedText != ACTIVATION_LAZY) {
                holder.createError(message("manifest.lang.specifyActivationOnly", ACTIVATION_LAZY), clause.textRange)
                annotated = true
            }

            if (clause.getDirectives().any { it.name != INCLUDE_DIRECTIVE && it.name != EXCLUDE_DIRECTIVE }) {
                holder.createError(
                    message("manifest.lang.specifyDirectivesOnly", "$INCLUDE_DIRECTIVE / $EXCLUDE_DIRECTIVE"),
                    clause.textRange
                )
                annotated = true
            }
        }

        return annotated
    }
}

object BundleClasspathParser : HeaderParser by OsgiHeaderParser {
    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        var annotated = false

        val moduleFile = header.module?.let { LocalFileSystem.getInstance().findFileByPath(it.getModuleDir()) }

        header.headerValues.mapNotNull { it as? Clause }.run {
            if (none { it.getValue()?.unwrappedText == "." }) {
                holder.createError(message("manifest.lang.classpathMustConDot"), header.textRange)
                annotated = true
            }

            forEach { clause ->
                val valuePart = clause.getValue()
                if (valuePart == null || valuePart.unwrappedText.isBlank()) {
                    holder.createError(
                        message("manifest.lang.invalidBlank"), valuePart?.highlightingRange ?: clause.textRange
                    )
                    annotated = true
                } else if (moduleFile?.findFileByRelativePath(valuePart.unwrappedText)?.exists() != true) {
                    holder.createError(
                        message("manifest.lang.notExist", valuePart.unwrappedText), valuePart.highlightingRange
                    )
                    annotated = true
                }
            }
        }

        return annotated
    }
}

object ExportPackageParser : HeaderParser by BasePackageParser {
    private val tokenFilter = TokenSet.create(ManifestTokenType.HEADER_VALUE_PART)

    override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
        if (BasePackageParser.annotate(header, holder)) return true

        var annotated = false

        header.headerValues.mapNotNull { it as? Clause }
            .mapNotNull { it.getDirectives().firstOrNull { directive -> USES_DIRECTIVE == directive.name } }
            .mapNotNull { it.getValueElement() }.forEach { valuePart ->
                val text = valuePart.unwrappedText
                var start = if (text.startsWith('"')) 1 else 0
                val length = text.length - if (text.endsWith('"')) 1 else 0
                val offset = valuePart.textOffset

                while (start < length) {
                    val end = text.indexOf(',', start).takeIf { it > -1 } ?: length
                    val range = TextRange.create(start, end)
                    start = end + 1

                    val packageName = range.substring(text).replace("\\s".toRegex(), "")
                    if (packageName.isBlank()) {
                        holder.createError(message("manifest.lang.invalidReference"), range.shiftRight(offset))
                        annotated = true
                    } else if (PsiHelper.resolvePackage(header, packageName).isEmpty()) {
                        holder.createError(
                            message("manifest.lang.cannotResolvePackage", packageName), range.shiftRight(offset)
                        )
                        annotated = true
                    }
                }
            }

        return annotated
    }

    override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> {
        val parent = headerValuePart.parent
        if (parent is Clause) {
            val element = headerValuePart.originalElement.prevSibling
            if (element !is ManifestToken || element.tokenType != ManifestTokenType.SEMICOLON) {
                return BasePackageParser.getPackageReferences(headerValuePart)
            }
        } else if (parent is Directive && parent.name == USES_DIRECTIVE) {
            return headerValuePart.node.getChildren(tokenFilter).mapNotNull { it as? ManifestToken }
                .flatMap { BasePackageParser.getPackageReferences(it).toList() }.toTypedArray()
        }

        return PsiReference.EMPTY_ARRAY
    }
}
