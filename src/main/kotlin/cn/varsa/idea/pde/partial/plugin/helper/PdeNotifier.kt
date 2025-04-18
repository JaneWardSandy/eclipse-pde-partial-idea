package cn.varsa.idea.pde.partial.plugin.helper

import com.intellij.notification.*

object PdeNotifier {
  private const val important = "PDE-Important"
  private const val information = "PDE-Information"

  fun notification(title: String, message: String) =
    NotificationGroupManager.getInstance().getNotificationGroup(information)
      .createNotification(message, NotificationType.INFORMATION).setTitle(title)

  fun important(title: String, message: String) = NotificationGroupManager.getInstance().getNotificationGroup(important)
    .createNotification(message, NotificationType.WARNING).setTitle(title)
}
