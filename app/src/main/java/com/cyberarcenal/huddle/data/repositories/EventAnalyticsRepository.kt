// EventAnalyticsRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class EventAnalyticsRepository {
    private val api = ApiService.eventAnalyticsApi

    suspend fun getEventAnalytics(eventId: Int, endDate: String? = null, page: Int? = null, pageSize: Int? = null, startDate: String? = null): Result<PaginatedEventAnalytics> =
        safeApiCall { api.apiV1EventsEventsAnalyticsRetrieve(eventId, endDate, page, pageSize, startDate) }

    suspend fun getEventAnalyticsForDate(date: String, eventId: Int): Result<EventAnalytics> =
        safeApiCall { api.apiV1EventsEventsAnalyticsRetrieve2(date, eventId) }

    suspend fun getEventAnalyticsSummary(eventId: Int, days: Int? = null): Result<EventAnalyticsSummary> =
        safeApiCall { api.apiV1EventsEventsAnalyticsSummaryRetrieve(eventId, days) }
}