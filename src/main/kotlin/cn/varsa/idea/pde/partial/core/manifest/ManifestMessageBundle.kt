package cn.varsa.idea.pde.partial.core.manifest

import com.intellij.*
import org.jetbrains.annotations.*

@NonNls private const val BUNDLE = "messages.ManifestMessage"

object ManifestMessageBundle : DynamicBundle(BUNDLE) {
  @JvmStatic @Nls fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getMessage(key, params)

  @JvmStatic @Nls fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getLazyMessage(key, params)
}