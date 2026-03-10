package com.cyberarcenal.huddle.data.repositories.admin

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class AdminRepository {
    private val api = ApiService.v1Api

    // ========== ADMIN ACTIONS ==========

    /**
     * Ban a user.
     */
    suspend fun banUser(userId: Int, reason: String, durationDays: Int? = null): Result<Any> {
        val input = BanUserInput(userId = userId, reason = reason, durationDays = durationDays)
        return safeApiCall { api.v1AdminPannelActionsBanUserCreate(input) }
    }

    /**
     * Remove a piece of content (post or group).
     */
    suspend fun removeContent(
        contentType: RemoveContentInputContentTypeEnum,
        objectId: Int,
        reason: String
    ): Result<Any> {
        val input = RemoveContentInput(contentType = contentType, objectId = objectId, reason = reason)
        return safeApiCall { api.v1AdminPannelActionsRemoveContentCreate(input) }
    }

    /**
     * Warn a user.
     */
    suspend fun warnUser(
        userId: Int,
        reason: String,
        severity: SeverityEnum? = null
    ): Result<Any> {
        val input = WarnUserInput(userId = userId, reason = reason, severity = severity)
        return safeApiCall { api.v1AdminPannelActionsWarnUserCreate(input) }
    }

    // ========== ADMIN LOGS ==========

    /**
     * List admin logs with optional filters.
     */
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

    /**
     * Get a single admin log by ID.
     */
    suspend fun getAdminLog(logId: Int): Result<AdminLog> = safeApiCall {
        api.v1AdminPannelLogsRetrieve2(logId)
    }

    /**
     * Get recent admin actions.
     */
    suspend fun getRecentAdminLogs(
        days: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> = safeApiCall {
        api.v1AdminPannelLogsRecentRetrieve(days, page, pageSize)
    }

    /**
     * Search admin logs by query.
     */
    suspend fun searchAdminLogs(
        query: String,
        searchIn: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> = safeApiCall {
        api.v1AdminPannelLogsSearchRetrieve(query, page, pageSize, searchIn)
    }

    /**
     * Get admin logs related to a specific user.
     */
    suspend fun getUserAdminLogs(
        userId: Int,
        asAdmin: Boolean? = null,
        asTarget: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedAdminLog> = safeApiCall {
        api.v1AdminPannelLogsUserRetrieve(userId, asAdmin, asTarget, page, pageSize)
    }

    /**
     * Get statistics about admin actions.
     */
    suspend fun getAdminStatistics(
        adminUserId: Int? = null,
        days: Int? = null
    ): Result<AdminStatistics> = safeApiCall {
        api.v1AdminPannelLogsStatisticsRetrieve(adminUserId, days)
    }

    /**
     * Export admin logs as JSON.
     */
    suspend fun exportAdminLogs(
        startDate: String? = null,
        endDate: String? = null,
        format: String? = null
    ): Result<Any> = safeApiCall {
        api.v1AdminPannelLogsExportRetrieve(endDate, format, startDate)
    }

    /**
     * Delete old admin logs.
     */
    suspend fun cleanupAdminLogs(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupLogsInput(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AdminPannelLogsCleanupCreate(body) }
    }

    // ========== REPORTS ==========

    /**
     * Submit a new report for a piece of content.
     */
    suspend fun createReport(
        contentType: ContentTypeA0aEnum,
        objectId: Int,
        reason: String
    ): Result<ReportedContent> {
        val input = ReportContentInput(contentType = contentType, objectId = objectId, reason = reason)
        return safeApiCall { api.v1AdminPannelReportCreate(input) }
    }

    /**
     * List reports with optional filters.
     */
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

    /**
     * Get a single report by ID.
     */
    suspend fun getReport(reportId: Int): Result<ReportedContent> = safeApiCall {
        api.v1AdminPannelReportsRetrieve2(reportId)
    }

    /**
     * Get all pending reports.
     */
    suspend fun getPendingReports(
        contentType: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedReportedContent> = safeApiCall {
        api.v1AdminPannelReportsPendingRetrieve(contentType, page, pageSize)
    }

    /**
     * Search reports by query.
     */
    suspend fun searchReports(
        query: String,
        searchIn: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedReportedContent> = safeApiCall {
        api.v1AdminPannelReportsSearchRetrieve(query, page, pageSize, searchIn)
    }

    /**
     * Get report history for a specific user.
     */
    suspend fun getUserReportHistory(
        userId: Int,
        asReporter: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedReportedContent> = safeApiCall {
        api.v1AdminPannelReportsUserHistoryRetrieve(userId, asReporter, page, pageSize)
    }

    /**
     * Get urgent reports (multiple reports on the same content in a short time).
     */
    suspend fun getUrgentReports(
        hours: Int? = null,
        threshold: Int? = null
    ): Result<List<Any>> = safeApiCall {
        api.v1AdminPannelReportsUrgentRetrieve(hours, threshold)
    }

    /**
     * Get statistics about reports.
     */
    suspend fun getReportStatistics(
        contentType: String? = null,
        days: Int? = null
    ): Result<ReportStatistics> = safeApiCall {
        api.v1AdminPannelReportsStatisticsRetrieve(contentType, days)
    }

    /**
     * Generate a moderation report for a given period.
     */
    suspend fun getModerationReport(
        startDate: String? = null,
        endDate: String? = null
    ): Result<Any> = safeApiCall {
        api.v1AdminPannelReportsModerationReportRetrieve(endDate, startDate)
    }

    // ========== REPORT ACTIONS ==========

    /**
     * Dismiss a report without taking action.
     */
    suspend fun dismissReport(reportId: Int, reason: String? = null): Result<ReportedContent> {
        val body = DismissReportInput(reason = reason)
        return safeApiCall { api.v1AdminPannelReportsDismissCreate(reportId, body) }
    }

    /**
     * Resolve a report by taking an action.
     */
    suspend fun resolveReport(
        reportId: Int,
        action: ResolveReportInputActionEnum,
        resolutionDetails: String? = null
    ): Result<Any> {
        val input = ResolveReportInput(action = action, resolutionDetails = resolutionDetails)
        return safeApiCall { api.v1AdminPannelReportsResolveCreate(reportId, input) }
    }

    /**
     * Update the status of a report.
     */
    suspend fun updateReportStatus(
        reportId: Int,
        status: ReportStatusUpdateStatusEnum? = null,
        resolutionNotes: String? = null
    ): Result<ReportedContent> {
        val update = PatchedReportStatusUpdate(status = status, resolutionNotes = resolutionNotes)
        return safeApiCall { api.v1AdminPannelReportsUpdateStatusPartialUpdate(reportId, update) }
    }

    /**
     * Delete old resolved/dismissed reports.
     */
    suspend fun cleanupReports(daysToKeep: Int): Result<V1AdminPannelLogsCleanupCreate200Response> {
        val body = CleanupReportsInput(daysToKeep = daysToKeep)
        return safeApiCall { api.v1AdminPannelReportsCleanupCreate(body) }
    }
}