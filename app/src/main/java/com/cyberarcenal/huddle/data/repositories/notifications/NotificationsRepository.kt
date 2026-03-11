package com.cyberarcenal.huddle.data.repositories.notifications

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class NotificationsRepository {
    private val api = ApiService.v1Api

    suspend fun getNotifications(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedNotification> = safeApiCall {
        api.v1NotificationsRetrieve(page, pageSize)
    }

    suspend fun getNotification(notificationId: Int): Result<Notification> = safeApiCall {
        api.v1NotificationsRetrieve2(notificationId)
    }

    suspend fun updateNotification(
        notificationId: Int,
        isRead: Boolean
    ): Result<Notification> {
        val patch = PatchedNotificationRequest(
            isRead = isRead
        )
        return safeApiCall { api.v1NotificationsPartialUpdate(notificationId, patch) }
    }

    suspend fun deleteNotification(notificationId: Int): Result<Unit> = safeApiCall {
        api.v1NotificationsDestroy(notificationId)
    }

    suspend fun markNotificationRead(notificationId: Int): Result<Any> {
        val request = NotificationMarkReadRequest(id = notificationId, markAll = false)
        return safeApiCall { api.v1NotificationsMarkReadCreate(request) }
    }

    suspend fun markAllNotificationsRead(): Result<V1AdminPannelLogsCleanupCreate200Response> = safeApiCall {
        api.v1NotificationsMarkAllReadCreate()
    }

    suspend fun getUnreadCount(): Result<V1NotificationsUnreadCountRetrieve200Response> = safeApiCall {
        api.v1NotificationsUnreadCountRetrieve()
    }
}