package cn.varsa.idea.pde.partial.plugin.openapi.resolver

import com.intellij.openapi.extensions.*
import org.jetbrains.annotations.*

interface LibraryResolver<AREA : AreaInstance> {
  @get:Nls(capitalization = Nls.Capitalization.Title) val displayName: String

  fun preResolve(area: AREA) {}
  fun resolve(area: AREA)
  fun postResolve(area: AREA) {}
}
