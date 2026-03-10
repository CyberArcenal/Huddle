package com.cyberarcenal.huddle.data.repositories.notifications

import com.cyberarcenal.huddle.api.models.Notification
import com.cyberarcenal.huddle.api.models.NotificationMarkRead
import com.cyberarcenal.huddle.api.models.PaginatedNotification
import com.cyberarcenal.huddle.api.models.PatchedNotification
import com.cyberarcenal.huddle.api.models.V1NotificationsUnreadCountRetrieve200Response
import com.cyberarcenal.huddle.api.models.V1AdminPannelLogsCleanupCreate200Response
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class NotificationsRepository {
    private val api = ApiService.v1Api

    /**
     * Get paginated list of notifications for the current user.
     */
    suspend fun getNotifications(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedNotification> = safeApiCall {
        api.v1NotificationsRetrieve(page, pageSize)
    }

    /**
     * Get a single notification by ID.
     */
    suspend fun getNotification(notificationId: Int): Result<Notification> = safeApiCall {
        api.v1NotificationsRetrieve2(notificationId)
    }

    /**
     * Update a notification (e.g., mark as read).
     */
    suspend fun updateNotification(
        notificationId: Int,
        isRead: Boolean
    ): Result<Notification> {
        // The API expects a PatchedNotification with isRead field.
        // We'll create a simple map or use the generated PatchedNotification.
        // For simplicity, we'll use a map (the API accepts any object).
        val patch = PatchedNotification(
            isRead = isRead
        )
        return safeApiCall { api.v1NotificationsPartialUpdate(notificationId, patch) }
    }

    /**
     * Delete a notification.
     */
    suspend fun deleteNotification(notificationId: Int): Result<Unit> = safeApiCall {
        api.v1NotificationsDestroy(notificationId)
    }

    /**
     * Mark a single notification as read by its ID.
     */
    suspend fun markNotificationRead(notificationId: Int): Result<Any> {
        val request = NotificationMarkRead(id = notificationId, markAll = false)
        return safeApiCall { api.v1NotificationsMarkReadCreate(request) }
    }

    /**
     * Mark all unread notifications as read.
     */
    suspend fun markAllNotificationsRead(): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1NotificationsMarkAllReadCreate()
    }

    /**
     * Get the number of unread notifications for the current user.
     */
    suspend fun getUnreadCount(): Result<V1NotificationsUnreadCountRetrieve200Response> = safeApiCall {
        api.v1NotificationsUnreadCountRetrieve()
    }
}