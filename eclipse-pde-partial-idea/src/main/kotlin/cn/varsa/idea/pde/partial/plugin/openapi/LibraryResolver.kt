package cn.varsa.idea.pde.partial.plugin.openapi

import com.intellij.openapi.extensions.*
import com.intellij.openapi.progress.*
import org.jetbrains.annotations.*

interface LibraryResolver<AREA : AreaInstance> {
    @get:Nls(capitalization = Nls.Capitalization.Title) val displayName: String

    fun preResolve(area: AREA, indicator: ProgressIndicator) {}
    fun resolve(area: AREA, indicator: ProgressIndicator)
    fun postResolve(area: AREA, indicator: ProgressIndicator) {}
}
