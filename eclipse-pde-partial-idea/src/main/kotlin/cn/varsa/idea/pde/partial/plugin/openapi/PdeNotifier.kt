package cn.varsa.idea.pde.partial.plugin.openapi

import com.intellij.notification.*
import com.intellij.openapi.project.*

class PdeNotifier(private val project: Project) {
    companion object {
        fun getInstance(project: Project): PdeNotifier = project.getService(PdeNotifier::class.java)

        private const val important = "PDE-Important"
        private const val information = "PDE-Information"
    }

    fun notification(title: String, message: String) =
        NotificationGroupManager.getInstance().getNotificationGroup(information)
            .createNotification(title, message, NotificationType.INFORMATION).notify(project)

    fun important(title: String, message: String) =
        NotificationGroupManager.getInstance().getNotificationGroup(important)
            .createNotification(title, message, NotificationType.WARNING).notify(project)
}
