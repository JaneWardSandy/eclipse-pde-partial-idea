package cn.varsa.idea.pde.partial.manifest.lang.parser

import cn.varsa.idea.pde.partial.common.Constants.Eclipse.X_FRIENDS_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.Eclipse.X_INTERNAL_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_HOST
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.USES_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.extension.*
import cn.varsa.idea.pde.partial.common.version.*
import cn.varsa.idea.pde.partial.core.manifest.*
import cn.varsa.idea.pde.partial.manifest.lang.*
import cn.varsa.idea.pde.partial.manifest.psi.*
import cn.varsa.idea.pde.partial.message.*
import com.intellij.lang.annotation.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.tree.*
import com.intellij.psi.util.*
import org.jetbrains.lang.manifest.psi.*

object ExportPackageParser : BasePackageParser() {
  private val tokenFilter = TokenSet.create(ManifestTokenType.HEADER_VALUE_PART)

  override fun getReferences(headerValuePart: HeaderValuePart): Array<PsiReference> {
    val parent = headerValuePart.parent

    if (parent is ManifestHeaderPart.Clause) {
      val element = headerValuePart.originalElement.prevSibling
      if (element !is ManifestToken || element.tokenType != ManifestTokenType.SEMICOLON) {
        return getPackageReferences(headerValuePart)
      }
    } else if (parent is ManifestHeaderPart.Directive && parent.name == USES_DIRECTIVE) {
      return headerValuePart.node.getChildren(tokenFilter).mapNotNull { it as? ManifestToken? }
        .flatMap { getPackageReferences(it).toList() }.toTypedArray()
    }

    return PsiReference.EMPTY_ARRAY
  }

  override fun checkValuePart(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    if (super.checkValuePart(clause, holder)) return true

    val value = clause.getValue() ?: return false
    val packageName = value.unwrappedText


    val fragmentHost =
      PsiTreeUtil.getChildrenOfTypeAsList(PsiTreeUtil.getParentOfType(clause, Section::class.java), Header::class.java)
        .firstOrNull { it.name == FRAGMENT_HOST }?.headerValue?.let { it as? ManifestHeaderPart.Clause? }
        ?: return false

    val hostBundleSymbolicName = fragmentHost.getValue()?.unwrappedText ?: return false
    val versionRange = try {
      fragmentHost.getAttributes().firstOrNull { it.name == BUNDLE_VERSION_ATTRIBUTE }
        ?.getValueElement()?.unwrappedText?.parseVersionRange()
    } catch (e: Exception) {
      VersionRange.ANY_VERSION_RANGE
    }

    val hostManifest = BundleManifestIndex.getManifestBySymbolicName(
      hostBundleSymbolicName,
      clause.project
    ).values.sortedByDescending { it.bundleVersion }
      .firstOrNull { versionRange == null || it.bundleVersion in versionRange }
    val hostExtensibleAPI = hostManifest?.eclipseExtensibleAPI
    val hostExportPackages = hostManifest?.exportPackage?.attributes?.keys?.map { it.removeSuffix(".*") }


    if (hostExportPackages != null && packageName !in hostExportPackages && hostExtensibleAPI != true) {
      holder.newAnnotation(HighlightSeverity.WARNING, ManifestBundle.message("manifest.lang.additionalAPIToHost"))
        .range(value.highlightingRange).create()
      return true
    }

    return false
  }

  override fun checkAttributes(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val attributes = clause.getAttributes()
    if (attributes.isEmpty()) return false

    val attribute = attributes[0]
    if (attributes.size > 1 || attribute.name != VERSION_ATTRIBUTE) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.specifyOnly", VERSION_ATTRIBUTE, "Attributes")
      ).range(attribute.textRange).create()
      return true
    }

    return CommonManifestHeaderParser.checkVersion(attribute, holder)
  }

  override fun checkDirectives(clause: ManifestHeaderPart.Clause, holder: AnnotationHolder): Boolean {
    val directives = clause.getDirectives()

    val names = directives.map { it.name }
    if (names.count { it == USES_DIRECTIVE } > 1) {
      holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", USES_DIRECTIVE))
        .range(clause.textRange).create()
      return true
    }
    if (names.count { it == X_INTERNAL_DIRECTIVE } > 1) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", X_INTERNAL_DIRECTIVE)
      ).range(clause.textRange).create()
      return true
    }
    if (names.count { it == X_FRIENDS_DIRECTIVE } > 1) {
      holder.newAnnotation(
        HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.duplicate", X_FRIENDS_DIRECTIVE)
      ).range(clause.textRange).create()
      return true
    }

    var annotate = false
    for (directive in directives) {
      val value = directive.getValueElement()
      val text = value?.unwrappedText
      if (text.isNullOrBlank()) {
        holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
          .range(value?.highlightingRange ?: directive.textRange).create()
        return true
      }

      val directiveName = directive.name
      if (directiveName == X_INTERNAL_DIRECTIVE) {
        if (text != true.toString()) {
          holder.newAnnotation(
            HighlightSeverity.ERROR,
            ManifestBundle.message("manifest.lang.shouldBe", X_INTERNAL_DIRECTIVE, true.toString())
          ).range(value.highlightingRange).create()
          annotate = true
        }
      } else if (directiveName == USES_DIRECTIVE || directiveName == X_FRIENDS_DIRECTIVE) {
        if (!text.surroundingWith('"')) {
          holder.newAnnotation(
            HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.shouldBe", USES_DIRECTIVE, "quoted")
          ).range(value.highlightingRange).create()
          annotate = true
        }

        val strings = text.replace("\\s", "").unquote().split(',').map { it.trim() }

        val manifests = if (directiveName == X_FRIENDS_DIRECTIVE) {
          BundleManifestIndex.getAllManifestBySymbolicNames(strings, clause.project)
            .mapNotNull { it.value.bundleSymbolicName?.key }
        } else emptyList()

        for (name in strings) {
          val start = text.indexOf(name)
          val end = start + name.length
          val range = TextRange.create(start, end).shiftRight(value.textOffset)

          if (name.isBlank()) {
            holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.invalidBlank"))
              .range(range).create()
            annotate = true
            continue
          }

          if (directiveName == USES_DIRECTIVE) {
            val empty = JavaPsiFacade.getInstance(clause.project).findPackage(name)
              ?.getDirectories(ProjectScope.getAllScope(clause.project)).isNullOrEmpty()
            if (empty) {
              holder.newAnnotation(
                HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.cannotResolvePackage", name)
              ).range(TextRange.create(start, end).shiftRight(value.textOffset)).create()
              annotate = true
            }
          } else if (name !in manifests) {
            holder.newAnnotation(HighlightSeverity.ERROR, ManifestBundle.message("manifest.lang.bundleNotExists", name))
              .range(range).create()
            annotate = true
          }
        }
      } else {
        holder.newAnnotation(
          HighlightSeverity.ERROR, ManifestBundle.message(
            "manifest.lang.specifyOnly", "$USES_DIRECTIVE/$X_INTERNAL_DIRECTIVE/$X_FRIENDS_DIRECTIVE", "Directives"
          )
        ).range(directive.textRange).create()
        annotate = true
      }
    }

    return annotate
  }
}