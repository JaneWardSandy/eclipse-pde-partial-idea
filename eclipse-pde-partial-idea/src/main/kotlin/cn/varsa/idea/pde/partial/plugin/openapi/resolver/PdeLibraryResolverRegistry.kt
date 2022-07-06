package cn.varsa.idea.pde.partial.plugin.openapi.resolver

import cn.varsa.idea.pde.partial.plugin.i18n.EclipsePDEPartialBundles.message
import cn.varsa.idea.pde.partial.plugin.support.*
import com.intellij.openapi.application.*
import com.intellij.openapi.extensions.*
import com.intellij.openapi.module.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*

class PdeLibraryResolverRegistry {

  companion object {
    val instance: PdeLibraryResolverRegistry
      get() = ApplicationManager.getApplication().getService(PdeLibraryResolverRegistry::class.java)
  }

  fun resolveProjectAndModule(
    project: Project,
    onSuccess: () -> Unit = {},
    onCancel: () -> Unit = {},
    onThrowable: (Throwable) -> Unit = { _ -> },
    onFinished: () -> Unit = {}
  ) {
    object : BackgroundResolvable {
      override fun resolve(project: Project, indicator: ProgressIndicator) {
        indicator.checkCanceled()
        indicator.isIndeterminate = false
        indicator.fraction = 0.0

        val pdeModules = project.allPDEModules()

        val step = 1 / (2 + pdeModules.size)
        resolveProject(project, indicator)
        indicator.fraction += step

        pdeModules.forEach {
          indicator.checkCanceled()
          resolveModule(it, indicator)
          indicator.fraction += step
        }

        indicator.fraction = 1.0
      }
    }.backgroundResolve(project, onSuccess, onCancel, onThrowable, onFinished)
  }

  private fun <AREA : AreaInstance> callExtensions(
    area: AREA, indicator: ProgressIndicator, extensions: List<LibraryResolver<AREA>>
  ) {
    val areaName = when (area) {
      is Project -> area.name
      is Module -> area.name
      is Application -> "Application"
      else -> "Unknown"
    }

    extensions.forEach {
      indicator.text = message("resolver.preResolve", areaName, it.displayName)
      indicator.checkCanceled()
      it.preResolve(area)
    }

    extensions.forEach {
      indicator.text = message("resolver.resolve", areaName, it.displayName)
      indicator.checkCanceled()
      it.resolve(area)
    }

    extensions.forEach {
      indicator.text = message("resolver.postResolve", areaName, it.displayName)
      indicator.checkCanceled()
      it.postResolve(area)
    }
  }

  fun resolveProject(project: Project, indicator: ProgressIndicator) {
    indicator.checkCanceled()
    callExtensions(project, indicator, TargetPlatformLibraryResolver.EP_NAME.extensionList)
  }

  fun resolveModule(module: Module, indicator: ProgressIndicator) {
    indicator.checkCanceled()
    callExtensions(module, indicator, ManifestLibraryResolver.EP_NAME.extensionList)

    indicator.checkCanceled()
    callExtensions(module, indicator, BuildLibraryResolver.EP_NAME.extensionList)
  }
}
