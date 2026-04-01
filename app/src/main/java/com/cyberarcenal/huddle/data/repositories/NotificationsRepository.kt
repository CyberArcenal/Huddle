package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class NotificationsRepository {
    private val api = ApiService.notificationsApi

    suspend fun deleteNotification(id: Int): Result<NotificationDeleteResponse> =
        safeApiCall { api.apiV1NotificationsDestroy(id) }

    suspend fun markAllRead(): Result<NotificationMarkAllReadResponse> =
        safeApiCall { api.apiV1NotificationsMarkAllReadCreate() }

    suspend fun markRead(request: NotificationMarkReadRequest? = null): Result<NotificationMarkReadResponse> =
        safeApiCall { api.apiV1NotificationsMarkReadCreate(request) }

    suspend fun partialUpdate(id: Int, request: PatchedNotificationRequest? = null): Result<NotificationUpdateResponse> =
        safeApiCall { api.apiV1NotificationsPartialUpdate(id, request) }

    suspend fun getNotifications(page: Int? = null, pageSize: Int? = null): Result<NotificationListResponse> =
        safeApiCall { api.apiV1NotificationsRetrieve(page, pageSize) }

    suspend fun getNotification(id: Int): Result<NotificationDetailResponse> =
        safeApiCall { api.apiV1NotificationsRetrieve2(id) }

    suspend fun getUnreadCount(): Result<NotificationUnreadCountResponse> =
        safeApiCall { api.apiV1NotificationsUnreadCountRetrieve() }
}