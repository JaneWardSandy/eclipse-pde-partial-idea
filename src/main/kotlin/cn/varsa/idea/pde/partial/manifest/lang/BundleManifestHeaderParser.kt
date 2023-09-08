package cn.varsa.idea.pde.partial.manifest.lang

import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.ManifestBundle
import com.intellij.lang.*
import com.intellij.lang.annotation.*
import com.intellij.psi.tree.TokenSet
import org.jetbrains.lang.manifest.header.impl.StandardHeaderParser
import org.jetbrains.lang.manifest.parser.ManifestParser.HEADER_END_TOKENS
import org.jetbrains.lang.manifest.psi.*
import org.jetbrains.lang.manifest.psi.ManifestTokenType.*

abstract class BundleManifestHeaderParser : StandardHeaderParser() {

  override fun parse(builder: PsiBuilder) {
    while (!builder.eof()) {
      if (!parseClause(builder)) break

      val tokenType = builder.tokenType
      if (tokenType in HEADER_END_TOKENS) break
      if (COMMA == tokenType) builder.advanceLexer()
    }
  }

  override fun annotate(header: Header, holder: AnnotationHolder): Boolean {
    var annotate = false

    val clauses = header.headerValues.mapNotNull { it as? ManifestHeaderPart.Clause? }
    if (!allowMultiClauses() && clauses.size > 1) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.multipleClause"))
        .range(header.textRange)
        .create()
      annotate = true
    }
    if (checkClauses(header, clauses, holder)) annotate = true

    for (clause in clauses) {
      val value = clause.getValue()

      if (value == null || value.unwrappedText.isBlank()) {
        holder
          .newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
          .range(value?.highlightingRange ?: clause.textRange)
          .create()
        annotate = true
      }

      if (checkValuePart(clause, holder)) annotate = true
      if (checkAttributes(clause, holder)) annotate = true
      if (checkDirectives(clause, holder)) annotate = true
    }

    return annotate
  }

  protected open fun allowMultiClauses(): Boolean = true
  protected open fun checkClauses(
    header: Header,
    clauses: List<ManifestHeaderPart.Clause>,
    holder: AnnotationHolder,
  ): Boolean = false

  protected open fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean = false
  protected open fun checkAttributes(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean = false
  protected open fun checkDirectives(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean = false

  private fun parseClause(builder: PsiBuilder): Boolean {
    val clause = builder.mark()
    var result = true

    while (!builder.eof()) {
      if (!parseSubClause(builder, false)) {
        result = false
        break
      }

      val tokenType = builder.tokenType
      if (tokenType in clauseEndTokens) break
      if (SEMICOLON == tokenType) builder.advanceLexer()
    }

    clause.done(BundleManifestElementType.CLAUSE)
    return result
  }

  private fun parseSubClause(builder: PsiBuilder, assigment: Boolean = true): Boolean {
    val mark = builder.mark()
    var result = true

    while (!builder.eof()) {
      val tokenType = builder.tokenType
      if (tokenType in subClauseEndTokens) break
      if (QUOTE == tokenType) {
        parseQuotedString(builder)
      } else if (!assigment && EQUALS == tokenType) {
        mark.done(ManifestElementType.HEADER_VALUE_PART)
        return parseAttribute(builder, mark.precede())
      } else if (!assigment && COLON == tokenType) {
        mark.done(ManifestElementType.HEADER_VALUE_PART)
        return parseDirective(builder, mark.precede())
      } else {
        val lastTokenType = builder.tokenType
        builder.advanceLexer()

        if (NEWLINE == lastTokenType && SIGNIFICANT_SPACE != builder.tokenType) {
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
    } while (!builder.eof() && builder.tokenType !in HEADER_END_TOKENS && !PsiBuilderUtil.expect(builder, QUOTE))
  }

  private fun parseAttribute(builder: PsiBuilder, mark: PsiBuilder.Marker): Boolean {
    builder.advanceLexer()
    val result = parseSubClause(builder)
    mark.done(BundleManifestElementType.ATTRIBUTE)
    return result
  }

  private fun parseDirective(builder: PsiBuilder, mark: PsiBuilder.Marker): Boolean {
    builder.advanceLexer()

    if (PsiBuilderUtil.expect(builder, NEWLINE)) {
      PsiBuilderUtil.expect(builder, SIGNIFICANT_SPACE)
    }
    PsiBuilderUtil.expect(builder, EQUALS)

    val result = parseSubClause(builder)
    mark.done(BundleManifestElementType.DIRECTIVE)
    return result
  }

  companion object {
    private val clauseEndTokens = TokenSet.orSet(HEADER_END_TOKENS, TokenSet.create(COMMA))
    private val subClauseEndTokens = TokenSet.orSet(clauseEndTokens, TokenSet.create(SEMICOLON))
  }
}