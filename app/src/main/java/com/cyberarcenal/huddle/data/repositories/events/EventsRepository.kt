package com.cyberarcenal.huddle.data.repositories.events

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class EventsRepository {
    private val api = ApiService.v1Api

    // ========== EVENTS ==========

    suspend fun getEvents(
        daysAhead: Int? = null,
        groupId: Int? = null,
        organizerId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        type: String? = null,
        upcoming: Boolean? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsRetrieve(daysAhead, groupId, organizerId, page, pageSize, type, upcoming)
    }

    suspend fun createEvent(eventCreate: EventCreateRequest): Result<EventDetail> = safeApiCall {
        api.v1EventsEventsCreate(eventCreate)
    }

    suspend fun getEvent(eventId: Int): Result<EventDetail> = safeApiCall {
        api.v1EventsEventsRetrieve2(eventId)
    }

    suspend fun updateEvent(eventId: Int, eventList: EventList): Result<EventList> = safeApiCall {
        api.v1EventsEventsUpdate(eventId, eventList)
    }

    suspend fun partialUpdateEvent(eventId: Int, patchedEventList: EventList): Result<EventList> = safeApiCall {
        api.v1EventsEventsPartialUpdate(eventId, patchedEventList)
    }

    suspend fun deleteEvent(eventId: Int): Result<Unit> = safeApiCall {
        api.v1EventsEventsDestroy(eventId)
    }

    // ========== ATTENDANCE / RSVP ==========

    suspend fun getEventAttendees(
        eventId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        status: String? = null
    ): Result<PaginatedEventAttendanceWithUser> = safeApiCall {
        api.v1EventsEventsAttendeesRetrieve(eventId, page, pageSize, status)
    }

    suspend fun rsvpToEvent(eventId: Int, status: StatusDecEnum): Result<EventAttendance> {
        val create = EventAttendanceCreateRequest(event = eventId, status = status)
        return safeApiCall { api.v1EventsEventsAttendeesCreate(eventId, create) }
    }

    suspend fun getAttendance(eventId: Int, userId: Int): Result<EventAttendance> = safeApiCall {
        api.v1EventsEventsAttendanceRetrieve2(eventId, userId)
    }

    suspend fun updateAttendance(eventId: Int, userId: Int, status: StatusDecEnum): Result<EventAttendance> {
        val update = EventAttendanceUpdateRequest(status = status)
        return safeApiCall { api.v1EventsEventsAttendanceUpdate2(eventId, userId, update) }
    }

    suspend fun deleteAttendance(eventId: Int, userId: Int): Result<Unit> = safeApiCall {
        api.v1EventsEventsAttendanceDestroy2(eventId, userId)
    }

    // ========== EVENT STATISTICS & ANALYTICS ==========

    suspend fun getEventStatistics(eventId: Int): Result<EventStatistics> = safeApiCall {
        api.v1EventsEventsStatisticsRetrieve(eventId)
    }

    suspend fun getEventAnalytics(
        eventId: Int,
        startDate: String? = null,
        endDate: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventAnalytics> = safeApiCall {
        api.v1EventsEventsAnalyticsRetrieve(eventId, endDate, page, pageSize, startDate)
    }

    suspend fun getEventAnalyticsByDate(eventId: Int, date: String): Result<EventAnalytics> = safeApiCall {
        api.v1EventsEventsAnalyticsRetrieve2(date, eventId)
    }

    suspend fun getEventAnalyticsSummary(eventId: Int, days: Int? = null): Result<EventAnalyticsSummary> = safeApiCall {
        api.v1EventsEventsAnalyticsSummaryRetrieve(eventId, days)
    }

    // ========== EVENT SEARCH & FILTERS ==========

    suspend fun searchEvents(
        q: String? = null,
        location: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        type: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsSearchRetrieve(endDate, location, page, pageSize, q, startDate, type)
    }

    suspend fun getUpcomingEvents(
        daysAhead: Int? = null,
        groupId: Int? = null,
        type: String? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsUpcomingRetrieve(daysAhead, groupId, page, pageSize, type, userId)
    }

    suspend fun getPastEvents(
        daysBack: Int? = null,
        groupId: Int? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsPastRetrieve(daysBack, groupId, page, pageSize, userId)
    }

    suspend fun getGroupEvents(
        groupId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsGroupRetrieve(groupId, page, pageSize, upcomingOnly)
    }

    suspend fun getEventsOrganizedByUser(
        userId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsOrganizedRetrieve2(userId, page, pageSize, upcomingOnly)
    }

    suspend fun getEventsByType(
        eventType: String,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsTypeRetrieve(eventType, page, pageSize, upcomingOnly)
    }

    // ========== FEATURED & RECOMMENDED ==========

    suspend fun getFeaturedEvents(
        daysAhead: Int? = null,
        limit: Int? = null,
        minAttendees: Int? = null
    ): Result<List<EventList>> = safeApiCall {
        api.v1EventsEventsFeaturedList(daysAhead, limit, minAttendees)
    }

    suspend fun getRecommendedEvents(limit: Int? = null): Result<List<EventList>> = safeApiCall {
        api.v1EventsEventsRecommendedList(limit)
    }

    // ========== USER EVENT HISTORY ==========

    suspend fun getUserAttendingEvents(
        userId: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        status: String? = null,
        upcomingOnly: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventAttendanceWithUser> = safeApiCall {
        api.v1EventsUserEventsRetrieve(endDate, page, pageSize, startDate, status, upcomingOnly, userId)
    }

    suspend fun getUserEventStatistics(userId: Int? = null): Result<UserAttendanceStatistics> = safeApiCall {
        api.v1EventsUserEventsStatisticsRetrieve(userId)
    }
}