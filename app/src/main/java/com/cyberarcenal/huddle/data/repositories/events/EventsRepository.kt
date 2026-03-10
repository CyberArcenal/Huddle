package com.cyberarcenal.huddle.data.repositories.events

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class EventsRepository {
    private val api = ApiService.v1Api

    // ========== EVENTS ==========

    /**
     * List events with optional filters.
     */
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

    /**
     * Create a new event.
     */
    suspend fun createEvent(eventCreate: EventCreate): Result<EventDetail> = safeApiCall {
        api.v1EventsEventsCreate(eventCreate)
    }

    /**
     * Get a single event by ID.
     */
    suspend fun getEvent(eventId: Int): Result<EventDetail> = safeApiCall {
        api.v1EventsEventsRetrieve2(eventId)
    }

    /**
     * Update all fields of an event.
     */
    suspend fun updateEvent(eventId: Int, eventList: EventList): Result<EventList> = safeApiCall {
        api.v1EventsEventsUpdate(eventId, eventList)
    }

    /**
     * Partially update an event.
     */
    suspend fun partialUpdateEvent(eventId: Int, patchedEventList: PatchedEventList): Result<EventList> = safeApiCall {
        api.v1EventsEventsPartialUpdate(eventId, patchedEventList)
    }

    /**
     * Delete an event.
     */
    suspend fun deleteEvent(eventId: Int): Result<Unit> = safeApiCall {
        api.v1EventsEventsDestroy(eventId)
    }

    // ========== ATTENDANCE / RSVP ==========

    /**
     * List all attendees for an event, optionally filtered by status.
     */
    suspend fun getEventAttendees(
        eventId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        status: String? = null
    ): Result<PaginatedEventAttendanceWithUser> = safeApiCall {
        api.v1EventsEventsAttendeesRetrieve(eventId, page, pageSize, status)
    }

    /**
     * RSVP to an event (create attendance).
     */
    suspend fun rsvpToEvent(eventId: Int, status: StatusDecEnum): Result<EventAttendance> {
        val create = EventAttendanceCreate(event = eventId, status = status)
        return safeApiCall { api.v1EventsEventsAttendeesCreate(eventId, create) }
    }

    /**
     * Get attendance record for a specific user (or current user if userId omitted?).
     * The API has two variants: by id (event id) and by user id.
     * We'll use the one that takes both.
     */
    suspend fun getAttendance(eventId: Int, userId: Int): Result<EventAttendance> = safeApiCall {
        api.v1EventsEventsAttendanceRetrieve2(eventId, userId)
    }

    /**
     * Update attendance status.
     */
    suspend fun updateAttendance(eventId: Int, userId: Int, status: StatusDecEnum): Result<EventAttendance> {
        val update = EventAttendanceUpdate(status = status)
        return safeApiCall { api.v1EventsEventsAttendanceUpdate2(eventId, userId, update) }
    }

    /**
     * Remove attendance (un‑RSVP).
     */
    suspend fun deleteAttendance(eventId: Int, userId: Int): Result<Unit> = safeApiCall {
        api.v1EventsEventsAttendanceDestroy2(eventId, userId)
    }

    // ========== EVENT STATISTICS & ANALYTICS ==========

    /**
     * Get detailed statistics for an event.
     */
    suspend fun getEventStatistics(eventId: Int): Result<EventStatistics> = safeApiCall {
        api.v1EventsEventsStatisticsRetrieve(eventId)
    }

    /**
     * Get event analytics for a date range.
     */
    suspend fun getEventAnalytics(
        eventId: Int,
        startDate: String? = null,
        endDate: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventAnalytics> = safeApiCall {
        api.v1EventsEventsAnalyticsRetrieve(eventId, endDate, page, pageSize, startDate)
    }

    /**
     * Get analytics for a specific date.
     */
    suspend fun getEventAnalyticsByDate(eventId: Int, date: String): Result<EventAnalytics> = safeApiCall {
        api.v1EventsEventsAnalyticsRetrieve2(date, eventId)
    }

    /**
     * Get summary of RSVP activity over the last N days.
     */
    suspend fun getEventAnalyticsSummary(eventId: Int, days: Int? = null): Result<EventAnalyticsSummary> = safeApiCall {
        api.v1EventsEventsAnalyticsSummaryRetrieve(eventId, days)
    }

    // ========== EVENT SEARCH & FILTERS ==========

    /**
     * Search events by query, location, date range.
     */
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

    /**
     * Get upcoming events with filters.
     */
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

    /**
     * Get past events with filters.
     */
    suspend fun getPastEvents(
        daysBack: Int? = null,
        groupId: Int? = null,
        userId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsPastRetrieve(daysBack, groupId, page, pageSize, userId)
    }

    /**
     * Get events for a group.
     */
    suspend fun getGroupEvents(
        groupId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsGroupRetrieve(groupId, page, pageSize, upcomingOnly)
    }

    /**
     * Get events organized by a specific user.
     */
    suspend fun getEventsOrganizedByUser(
        userId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsOrganizedRetrieve2(userId, page, pageSize, upcomingOnly)
    }

    /**
     * Get events of a specific type.
     */
    suspend fun getEventsByType(
        eventType: String,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<PaginatedEventList> = safeApiCall {
        api.v1EventsEventsTypeRetrieve(eventType, page, pageSize, upcomingOnly)
    }

    // ========== FEATURED & RECOMMENDED ==========

    /**
     * Get featured (most popular) events.
     */
    suspend fun getFeaturedEvents(
        daysAhead: Int? = null,
        limit: Int? = null,
        minAttendees: Int? = null
    ): Result<List<EventList>> = safeApiCall {
        api.v1EventsEventsFeaturedList(daysAhead, limit, minAttendees)
    }

    /**
     * Get personalized event recommendations.
     */
    suspend fun getRecommendedEvents(limit: Int? = null): Result<List<EventList>> = safeApiCall {
        api.v1EventsEventsRecommendedList(limit)
    }

    // ========== USER EVENT HISTORY ==========

    /**
     * Get events a user is attending, with optional filters.
     */
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

    /**
     * Get attendance statistics for a user.
     */
    suspend fun getUserEventStatistics(userId: Int? = null): Result<UserAttendanceStatistics> = safeApiCall {
        api.v1EventsUserEventsStatisticsRetrieve(userId)
    }
}