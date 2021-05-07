package cn.varsa.idea.pde.partial.plugin.i18n

import com.intellij.*
import org.jetbrains.annotations.*

private const val bundlePath = "messages.EclipsePDEPartialBundles_locale"

object EclipsePDEPartialBundles : DynamicBundle(bundlePath) {
    @Nls fun message(@PropertyKey(resourceBundle = bundlePath) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
