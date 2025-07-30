package cn.varsa.idea.pde.partial.manifest.completion

import cn.varsa.idea.pde.partial.common.Constants.Eclipse.ECLIPSE_EXTENSIBLE_API
import cn.varsa.idea.pde.partial.common.Constants.Eclipse.X_FRIENDS_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.Eclipse.X_INTERNAL_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.ACTIVATION_LAZY
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_ACTIVATIONPOLICY
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_REQUIREDEXECUTIONENVIRONMENT
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_SYMBOLICNAME
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_SYMBOLICNAME_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.BUNDLE_VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.EXPORT_PACKAGE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_ATTACHMENT_ALWAYS
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_ATTACHMENT_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_ATTACHMENT_NEVER
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_ATTACHMENT_RESOLVETIME
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.FRAGMENT_HOST
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.IMPORT_PACKAGE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.REQUIRE_BUNDLE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_MANDATORY
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.RESOLUTION_OPTIONAL
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.SINGLETON_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.USES_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VERSION_ATTRIBUTE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_DIRECTIVE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_PRIVATE
import cn.varsa.idea.pde.partial.common.Constants.OSGI.Header.VISIBILITY_REEXPORT
import cn.varsa.idea.pde.partial.manifest.lang.parser.*
import com.intellij.codeInsight.completion.*

class BasicBundleManifestCompletionContributor : BundleManifestCompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      clause(BUNDLE_REQUIREDEXECUTIONENVIRONMENT),
      StringValueCompletionProvider(RequiredExecutionEnvironmentParser.javaExecutionEnvironments)
    )
    extend(CompletionType.BASIC, clause(REQUIRE_BUNDLE), BundleSymbolicNameCompletionProvider)
    extend(CompletionType.BASIC, clause(FRAGMENT_HOST), BundleSymbolicNameCompletionProvider)
    extend(CompletionType.BASIC, clause(BUNDLE_ACTIVATIONPOLICY), StringValueCompletionProvider(ACTIVATION_LAZY))

    extend(
      CompletionType.BASIC,
      header(REQUIRE_BUNDLE),
      HeaderParameterCompletionProvider(BUNDLE_VERSION_ATTRIBUTE, "$VISIBILITY_DIRECTIVE:", "$RESOLUTION_DIRECTIVE:")
    )
    extend(CompletionType.BASIC, header(FRAGMENT_HOST), HeaderParameterCompletionProvider(BUNDLE_VERSION_ATTRIBUTE))
    extend(
      CompletionType.BASIC,
      header(BUNDLE_SYMBOLICNAME),
      HeaderParameterCompletionProvider("$SINGLETON_DIRECTIVE:", "$FRAGMENT_ATTACHMENT_DIRECTIVE:")
    )
    extend(
      CompletionType.BASIC, header(EXPORT_PACKAGE), HeaderParameterCompletionProvider(
        VERSION_ATTRIBUTE,
        "$USES_DIRECTIVE:",
        "$X_INTERNAL_DIRECTIVE:",
        "$X_FRIENDS_DIRECTIVE:",
      )
    )
    extend(
      CompletionType.BASIC,
      header(IMPORT_PACKAGE),
      HeaderParameterCompletionProvider(VERSION_ATTRIBUTE, BUNDLE_SYMBOLICNAME_ATTRIBUTE, "$RESOLUTION_DIRECTIVE:")
    )

    extend(CompletionType.BASIC, attribute(BUNDLE_SYMBOLICNAME_ATTRIBUTE), BundleSymbolicNameCompletionProvider)

    extend(
      CompletionType.BASIC,
      directive(VISIBILITY_DIRECTIVE),
      StringValueCompletionProvider(VISIBILITY_PRIVATE, VISIBILITY_REEXPORT)
    )
    extend(
      CompletionType.BASIC,
      directive(RESOLUTION_DIRECTIVE),
      StringValueCompletionProvider(RESOLUTION_MANDATORY, RESOLUTION_OPTIONAL)
    )
    extend(
      CompletionType.BASIC,
      directive(SINGLETON_DIRECTIVE),
      StringValueCompletionProvider(true.toString(), false.toString())
    )
    extend(
      CompletionType.BASIC,
      directive(FRAGMENT_ATTACHMENT_DIRECTIVE),
      StringValueCompletionProvider(
        FRAGMENT_ATTACHMENT_ALWAYS,
        FRAGMENT_ATTACHMENT_RESOLVETIME,
        FRAGMENT_ATTACHMENT_NEVER,
      ),
    )

    extend(
      CompletionType.BASIC,
      valuePart(ECLIPSE_EXTENSIBLE_API),
      StringValueCompletionProvider(true.toString(), false.toString())
    )
  }
}