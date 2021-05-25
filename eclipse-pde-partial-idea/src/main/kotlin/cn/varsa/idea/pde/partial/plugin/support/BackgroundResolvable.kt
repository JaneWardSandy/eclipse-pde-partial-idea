package cn.varsa.idea.pde.partial.plugin.support

import cn.varsa.idea.pde.partial.plugin.i18n.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*

interface BackgroundResolvable {
    fun resolve(project: Project, indicator: ProgressIndicator)
    fun onSuccess() {}
    fun onCancel() {}
    fun onThrowable(throwable: Throwable) {}
    fun onFinished() {}

    fun backgroundResolve(
        project: Project,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {},
        onThrowable: (Throwable) -> Unit = { e -> thisLogger().error(e.message, e) },
        onFinished: () -> Unit = {}
    ) {
        object : Task.Backgroundable(
            project, EclipsePDEPartialBundles.message("config.target.service.resolving"), true, DEAF
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.checkCanceled()
                resolve(project, indicator)
            }

            override fun onSuccess() {
                super.onSuccess()
                onSuccess()
                this@BackgroundResolvable.onSuccess()
            }

            override fun onCancel() {
                super.onCancel()
                onCancel()
                this@BackgroundResolvable.onCancel()
            }

            override fun onThrowable(error: Throwable) {
                super.onThrowable(error)
                onThrowable(error)
                this@BackgroundResolvable.onThrowable(error)
            }

            override fun onFinished() {
                super.onFinished()
                onFinished()
                this@BackgroundResolvable.onFinished()
            }
        }.setCancelText(EclipsePDEPartialBundles.message("config.target.service.cancel")).queue()
    }
}
