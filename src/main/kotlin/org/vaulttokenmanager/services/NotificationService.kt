package org.vaulttokenmanager.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager

/**
 * Centralized service for displaying user notifications in IntelliJ's notification system.
 * Handles various notification types (error, warning, info) for vault operations.
 */
@Service
class NotificationService {
    private val NOTIFICATION_GROUP_ID = "Vault Token Notifications"

    /**
     * Displays a notification balloon in the IDE with the specified message and severity.
     * Automatically selects an appropriate project context for the notification.
     *
     * @param content The text message to display in the notification
     * @param type The severity level: ERROR (red), WARNING (yellow), or INFORMATION (gray)
     */
    fun showNotification(content: String, type: NotificationType) {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)

        val project = ProjectManager.getInstance().openProjects.firstOrNull()
            ?: ProjectManager.getInstance().defaultProject

        notificationGroup
            .createNotification(content, type)
            .notify(project)
    }

    companion object {
        /**
         * Gets the singleton instance of the NotificationService
         */
        @JvmStatic
        fun getInstance(): NotificationService = service()
    }
}
