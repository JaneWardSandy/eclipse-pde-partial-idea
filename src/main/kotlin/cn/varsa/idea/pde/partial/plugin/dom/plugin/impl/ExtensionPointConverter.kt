package cn.varsa.idea.pde.partial.plugin.dom.plugin.impl

import cn.varsa.idea.pde.partial.plugin.dom.config.*
import com.intellij.util.xml.*

class ExtensionPointConverter : ResolvingConverter<String>() {
  override fun toString(t: String?, context: ConvertContext): String? = t?.takeIf(getVariants(context)::contains)
  override fun fromString(s: String?, context: ConvertContext): String? = s?.takeIf(getVariants(context)::contains)
  override fun getVariants(context: ConvertContext): Collection<String> =
    ExtensionPointManagementService.getInstance(context.project).getExtensionPoints().sorted()
}
