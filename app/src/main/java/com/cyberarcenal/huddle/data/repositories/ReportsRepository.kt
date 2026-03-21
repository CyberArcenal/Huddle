// ReportsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class ReportsRepository {
    private val api = ApiService.reportsApi

    suspend fun createReport(request: ReportContentInputRequest): Result<ReportedContentDisplay> =
        safeApiCall { api.apiV1AdminPannelReportCreate(request) }

    suspend fun cleanupReports(request: CleanupReportsInputRequest? = null): Result<ApiV1AdminPannelLogsCleanupCreate200Response> =
        safeApiCall { api.apiV1AdminPannelReportsCleanupCreate(request) }

    suspend fun dismissReport(reportId: Int, request: DismissReportInputRequest? = null): Result<ReportedContentDisplay> =
        safeApiCall { api.apiV1AdminPannelReportsDismissCreate(reportId, request) }

    suspend fun getModerationReport(endDate: String? = null, startDate: String? = null): Result<ModerationReportResponse> =
        safeApiCall { api.apiV1AdminPannelReportsModerationReportRetrieve(endDate, startDate) }

    suspend fun getPendingReports(contentType: String? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedReportedContent> =
        safeApiCall { api.apiV1AdminPannelReportsPendingRetrieve(contentType, page, pageSize) }

    suspend fun resolveReport(reportId: Int, request: ResolveReportInputRequest): Result<ReportResolveResponse> =
        safeApiCall { api.apiV1AdminPannelReportsResolveCreate(reportId, request) }

    suspend fun getReports(
        contentType: String? = null,
        endDate: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        reporterId: Int? = null,
        startDate: String? = null,
        status: String? = null,
        unresolvedOnly: Boolean? = null
    ): Result<PaginatedReportedContent> =
        safeApiCall { api.apiV1AdminPannelReportsRetrieve(contentType, endDate, page, pageSize, reporterId, startDate, status, unresolvedOnly) }

    suspend fun getReport(reportId: Int): Result<ReportedContentDisplay> =
        safeApiCall { api.apiV1AdminPannelReportsRetrieve2(reportId) }

    suspend fun searchReports(query: String, page: Int? = null, pageSize: Int? = null, searchIn: String? = null): Result<PaginatedReportedContent> =
        safeApiCall { api.apiV1AdminPannelReportsSearchRetrieve(query, page, pageSize, searchIn) }

    suspend fun getReportStatistics(contentType: String? = null, days: Int? = null): Result<ReportStatistics> =
        safeApiCall { api.apiV1AdminPannelReportsStatisticsRetrieve(contentType, days) }

    suspend fun updateReportStatus(reportId: Int, request: PatchedReportStatusUpdateRequest? = null): Result<ReportedContentDisplay> =
        safeApiCall { api.apiV1AdminPannelReportsUpdateStatusPartialUpdate(reportId, request) }

    suspend fun getUrgentReports(hours: Int? = null, threshold: Int? = null): Result<List<UrgentReport>> =
        safeApiCall { api.apiV1AdminPannelReportsUrgentList(hours, threshold) }

    suspend fun getUserReportHistory(userId: Int, asReporter: Boolean? = null, page: Int? = null, pageSize: Int? = null): Result<PaginatedReportedContent> =
        safeApiCall { api.apiV1AdminPannelReportsUserHistoryRetrieve(userId, asReporter, page, pageSize) }
}