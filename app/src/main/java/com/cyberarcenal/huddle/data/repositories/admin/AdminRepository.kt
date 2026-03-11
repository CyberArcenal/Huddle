package com.cyberarcenal.huddle.data.repositories.admin

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class AdminRepository {
    private val api = ApiService.v1Api

    // ========== ADMIN ACTIONS ==========

    suspend fun banUser(userId: Int, reason: String, durationDays: Int? = null): Result<Any> {
        val input = BanUserInputRequest(userId = userId, reason = reason, durationDays = durationDays)
        return safeApiCall { api.v1AdminPannelActionsBanUserCreate(input) }
    }

    suspend fun removeContent(
        contentType: RemoveContentInputContentTypeEnum,
        objectId: Int,
        reason: String
    ): Result<Any> {
        val input = RemoveContentInputRequest(contentType = contentType, objectId = objectId, reason = reason)
        return safeApiCall { api.v1AdminPannelActionsRemoveContentCreate(input) }
    }

    suspend fun warnUser(
        userId: Int,
        reason: String,
        severity: SeverityEnum? = null
    ): Result<Any> {
        val input = WarnUserInputRequest(userId = userId, reason = reason, severity = severity)
        return safeApiCall { api.v1AdminPannelActionsWarnUserCreate(input) }
    }

    // ========== ADMIN LOGS ==========

    suspend fun getAdminLogs(
        action: String? = null,
        adminUserId: Int? = null,
        targetUserId: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> = safeApiCall {
        api.v1AdminPannelLogsRetrieve(action, adminUserId, endDate, page, pageSize, startDate, targetUserId)
    }

    suspend fun getAdminLog(logId: Int): Result<AdminLog> = safeApiCall {
        api.v1AdminPannelLogsRetrieve2(logId)
    }

    suspend fun getRecentAdminLogs(
        days: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> = safeApiCall {
        api.v1AdminPannelLogsRecentRetrieve(days, page, pageSize)
    }

    suspend fun searchAdminLogs(
        query: String,
        searchIn: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> = safeApiCall {
        api.v1AdminPannelLogsSearchRetrieve(query, page, pageSize, searchIn)
    }

    suspend fun getUserAdminLogs(
        userId: Int,
        asAdmin: Boolean? = null,
        asTarget: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> = safeApiCall {
        api.v1AdminPannelLogsUserRetrieve(userId, asAdmin, asTarget, page, pageSize)
    }

    suspend fun getAdminStatistics(
        adminUserId: Int? = null,
        days: Int? = null
    ): Result<AdminStatistics> = safeApiCall {
        api.v1AdminPannelLogsStatisticsRetrieve(adminUserId, days)
    }

    suspend fun exportAdminLogs(
        startDate: String? = null,
        endDate: String? = null,
        format: String? = null
    ): Result<Any> = safeApiCall {
        api.v1AdminPannelLogsExportRetrieve(endDate, format, startDate)
    }

    suspend fun cleanupAdminLogs(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupLogsInputRequest(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AdminPannelLogsCleanupCreate(body) }
    }

    // ========== REPORTS ==========

    suspend fun createReport(
        contentType: ContentTypeA0aEnum,
        objectId: Int,
        reason: String
    ): Result<ReportedContent> {
        val input = ReportContentInputRequest(contentType = contentType, objectId = objectId, reason = reason)
        return safeApiCall { api.v1AdminPannelReportCreate(input) }
    }

    suspend fun getReports(
        contentType: String? = null,
        reporterId: Int? = null,
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        unresolvedOnly: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedReportedContent> = safeApiCall {
        api.v1AdminPannelReportsRetrieve(contentType, endDate, page, pageSize, reporterId, startDate, status, unresolvedOnly)
    }

    suspend fun getReport(reportId: Int): Result<ReportedContent> = safeApiCall {
        api.v1AdminPannelReportsRetrieve2(reportId)
    }

    suspend fun getPendingReports(
        contentType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedReportedContent> = safeApiCall {
        api.v1AdminPannelReportsPendingRetrieve(contentType, page, pageSize)
    }

    suspend fun searchReports(
        query: String,
        searchIn: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedReportedContent> = safeApiCall {
        api.v1AdminPannelReportsSearchRetrieve(query, page, pageSize, searchIn)
    }

    suspend fun getUserReportHistory(
        userId: Int,
        asReporter: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedReportedContent> = safeApiCall {
        api.v1AdminPannelReportsUserHistoryRetrieve(userId, asReporter, page, pageSize)
    }

    suspend fun getUrgentReports(
        hours: Int? = null,
        threshold: Int? = null
    ): Result<List<Any>> = safeApiCall {
        api.v1AdminPannelReportsUrgentRetrieve(hours, threshold)
    }

    suspend fun getReportStatistics(
        contentType: String? = null,
        days: Int? = null
    ): Result<ReportStatistics> = safeApiCall {
        api.v1AdminPannelReportsStatisticsRetrieve(contentType, days)
    }

    suspend fun getModerationReport(
        startDate: String? = null,
        endDate: String? = null
    ): Result<Any> = safeApiCall {
        api.v1AdminPannelReportsModerationReportRetrieve(endDate, startDate)
    }

    // ========== REPORT ACTIONS ==========

    suspend fun dismissReport(reportId: Int, reason: String? = null): Result<ReportedContent> {
        val body = DismissReportInputRequest(reason = reason)
        return safeApiCall { api.v1AdminPannelReportsDismissCreate(reportId, body) }
    }

    suspend fun resolveReport(
        reportId: Int,
        action: ResolveReportInputActionEnum,
        resolutionDetails: String? = null
    ): Result<Any> {
        val input = ResolveReportInputRequest(action = action, resolutionDetails = resolutionDetails)
        return safeApiCall { api.v1AdminPannelReportsResolveCreate(reportId, input) }
    }

    suspend fun updateReportStatus(
        reportId: Int,
        status: ReportStatusUpdateStatusEnum? = null,
        resolutionNotes: String? = null
    ): Result<ReportedContent> {
        val update = PatchedReportStatusUpdateRequest(status = status, resolutionNotes = resolutionNotes)
        return safeApiCall { api.v1AdminPannelReportsUpdateStatusPartialUpdate(reportId, update) }
    }

    suspend fun cleanupReports(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupReportsInputRequest(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AdminPannelReportsCleanupCreate(body) }
    }
}