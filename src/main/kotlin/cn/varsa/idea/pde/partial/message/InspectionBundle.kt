package cn.varsa.idea.pde.partial.message

import com.intellij.DynamicBundle
import org.jetbrains.annotations.*

@NonNls private const val BUNDLE = "messages.Inspection"

object InspectionBundle : DynamicBundle(BUNDLE) {
  @JvmStatic
  @Nls
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

  @JvmStatic
  @Nls
  fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getLazyMessage(key, *params)
}