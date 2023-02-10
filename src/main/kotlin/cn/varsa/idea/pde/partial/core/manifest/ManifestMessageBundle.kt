package cn.varsa.idea.pde.partial.core.manifest

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls private const val BUNDLE = "messages.ManifestMessage"

object ManifestMessageBundle : DynamicBundle(BUNDLE) {
  @Nls fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, params)
  @Nls fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getLazyMessage(key, params)
}