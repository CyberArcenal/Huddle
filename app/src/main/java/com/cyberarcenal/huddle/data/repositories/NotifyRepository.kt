package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.NotifyApi
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class NotifyRepository {
    private val api: NotifyApi = ApiService.notifyApi

    suspend fun createLog(request: NotifyLogCreateRequest): Result<NotifyLogCreateResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsCreate(request) }

    suspend fun createLogWithId(id: Int, request: NotifyLogCreateRequest): Result<NotifyLogCreateResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsCreate2(id, request) }

    suspend fun deleteLog(): Result<NotifyLogDeleteResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsDestroy() }

    suspend fun deleteLogById(id: Int): Result<NotifyLogDeleteResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsDestroy2(id) }

    suspend fun partialUpdateLog(request: PatchedNotifyLogCreateRequest? = null): Result<NotifyLogUpdateResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsPartialUpdate(request) }

    suspend fun partialUpdateLogById(
        id: Int,
        request: PatchedNotifyLogCreateRequest? = null
    ): Result<NotifyLogUpdateResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsPartialUpdate2(id, request) }

    suspend fun resendNotification(id: Int): Result<NotifyLogResendResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsResendCreate(id) }

    suspend fun getLogs(id: Int, recipientEmail: String? = null, status: String? = null): Result<NotifyLogDetailResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsRetrieve(id, recipientEmail, status) }

    suspend fun getLogById(id: Int, recipientEmail: String? = null, status: String? = null): Result<NotifyLogDetailResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsRetrieve2(id, recipientEmail, status) }

    suspend fun retryNotification(id: Int): Result<NotifyLogRetryResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsRetryCreate(id) }

    suspend fun updateLog(request: NotifyLogCreateRequest): Result<NotifyLogUpdateResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsUpdate(request) }

    suspend fun updateLogById(id: Int, request: NotifyLogCreateRequest): Result<NotifyLogUpdateResponse> =
        safeApiCall { api.apiV1NotificationsNotifylogsUpdate2(id, request) }
}