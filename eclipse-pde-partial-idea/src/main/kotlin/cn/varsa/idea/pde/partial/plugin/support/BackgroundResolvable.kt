package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.plugin.i18n.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*

interface BackgroundResolvable {
    fun resolve(project: Project, indicator: ProgressIndicator)
    fun onSuccess(project: Project) {}
    fun onCancel(project: Project) {}
    fun onThrowable(project: Project, throwable: Throwable) {}
    fun onFinished(project: Project) {}

    fun backgroundResolve(
        project: Project,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {},
        onThrowable: (Throwable) -> Unit = { _ -> },
        onFinished: () -> Unit = {},
        taskOP: Task.() -> Unit = {}
    ) {
        object : Task.Backgroundable(project, EclipsePDEPartialBundles.message("config.target.service.resolving")) {
            override fun run(indicator: ProgressIndicator) {
                indicator.checkCanceled()
                resolve(project, indicator)
            }

            override fun onSuccess() {
                super.onSuccess()
                onSuccess()
                this@BackgroundResolvable.onSuccess(project)
            }

            override fun onCancel() {
                super.onCancel()
                onCancel()
                this@BackgroundResolvable.onCancel(project)
            }

            override fun onThrowable(error: Throwable) {
                super.onThrowable(error)
                onThrowable(error)
                this@BackgroundResolvable.onThrowable(project, error)
            }

            override fun onFinished() {
                super.onFinished()
                onFinished()
                this@BackgroundResolvable.onFinished(project)
            }
        }.setCancelText(EclipsePDEPartialBundles.message("config.target.service.cancel")).apply(taskOP).queue()
    }
}
