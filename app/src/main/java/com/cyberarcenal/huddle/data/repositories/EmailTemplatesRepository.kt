package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.apis.EmailTemplatesApi
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class EmailTemplatesRepository {
    private val api: EmailTemplatesApi = ApiService.templatesApi

    suspend fun createTemplate(request: EmailTemplateCreateRequest): Result<EmailTemplateCreateResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesCreate(request) }

    suspend fun createTemplateWithId(id: Int, request: EmailTemplateCreateRequest): Result<EmailTemplateCreateResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesCreate2(id, request) }

    suspend fun deleteTemplate(): Result<EmailTemplateDeleteResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesDestroy() }

    suspend fun deleteTemplateById(id: Int): Result<EmailTemplateDeleteResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesDestroy2(id) }

    suspend fun partialUpdateTemplate(request: PatchedEmailTemplateCreateRequest? = null): Result<EmailTemplateUpdateResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesPartialUpdate(request) }

    suspend fun partialUpdateTemplateById(
        id: Int,
        request: PatchedEmailTemplateCreateRequest? = null
    ): Result<EmailTemplateUpdateResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesPartialUpdate2(id, request) }

    suspend fun getTemplates(id: Int, name: String? = null): Result<EmailTemplateDetailResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesRetrieve(id, name) }

    suspend fun getTemplateById(id: Int, name: String? = null): Result<EmailTemplateDetailResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesRetrieve2(id, name) }

    suspend fun updateTemplate(request: EmailTemplateCreateRequest): Result<EmailTemplateUpdateResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesUpdate(request) }

    suspend fun updateTemplateById(id: Int, request: EmailTemplateCreateRequest): Result<EmailTemplateUpdateResponse> =
        safeApiCall { api.apiV1NotificationsEmailTemplatesUpdate2(id, request) }
}