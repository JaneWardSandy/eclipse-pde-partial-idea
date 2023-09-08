package cn.varsa.idea.pde.partial.core.extension

import com.intellij.notification.*

private const val IMPORTANT = "cn.varsa.idea.pde.partial.notification.important"

fun notificationImportant(): NotificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(IMPORTANT)
