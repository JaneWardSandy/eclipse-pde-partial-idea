package cn.varsa.idea.pde.partial.manifest.lang

import cn.varsa.idea.pde.partial.manifest.psi.BundleManifestElementType
import com.intellij.lang.*
import com.intellij.psi.tree.TokenSet
import org.jetbrains.lang.manifest.header.impl.StandardHeaderParser
import org.jetbrains.lang.manifest.parser.ManifestParser.HEADER_END_TOKENS
import org.jetbrains.lang.manifest.psi.ManifestElementType
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