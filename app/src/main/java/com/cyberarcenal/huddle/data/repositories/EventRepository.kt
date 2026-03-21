// EventRepository.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class EventRepository {
    private val api = ApiService.eventApi

    suspend fun createEvent(request: EventCreateRequest): Result<EventDetail> =
        safeApiCall { api.apiV1EventsEventsCreate(request) }

    suspend fun createEventAlt(request: EventCreateRequest): Result<EventDetail> =
        safeApiCall { api.apiV1EventsEventsCreateCreate(request) }

    suspend fun deleteEvent(id: Int): Result<Unit> =
        safeApiCall { api.apiV1EventsEventsDeleteDestroy(id) }

    suspend fun deleteEventAlt(id: Int): Result<Unit> =
        safeApiCall { api.apiV1EventsEventsDestroy(id) }

    suspend fun getFeaturedEvents(daysAhead: Int? = null, limit: Int? = null, minAttendees: Int? = null): Result<List<EventList>> =
        safeApiCall { api.apiV1EventsEventsFeaturedList(daysAhead, limit, minAttendees) }

    suspend fun getGroupEvents(groupId: Int, page: Int? = null, pageSize: Int? = null, upcomingOnly: Boolean? = null): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsGroupRetrieve(groupId, page, pageSize, upcomingOnly) }

    suspend fun getOrganizedEvents(page: Int? = null, pageSize: Int? = null, upcomingOnly: Boolean? = null, userId: Int? = null): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsOrganizedRetrieve(page, pageSize, upcomingOnly, userId) }

    suspend fun getOrganizedEventsForUser(userId: Int, page: Int? = null, pageSize: Int? = null, upcomingOnly: Boolean? = null, userId2: Int? = null): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsOrganizedRetrieve2(userId, page, pageSize, upcomingOnly, userId2) }

    suspend fun partialUpdateEvent(id: Int, request: PatchedEventUpdateRequest? = null): Result<EventDetail> =
        safeApiCall { api.apiV1EventsEventsPartialUpdate(id, request) }

    suspend fun getPastEvents(daysBack: Int? = null, groupId: Int? = null, page: Int? = null, pageSize: Int? = null, userId: Int? = null): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsPastRetrieve(daysBack, groupId, page, pageSize, userId) }

    suspend fun getRecommendedEvents(limit: Int? = null): Result<List<EventList>> =
        safeApiCall { api.apiV1EventsEventsRecommendedList(limit) }

    suspend fun getEvents(
        daysAhead: Int? = null,
        groupId: Int? = null,
        organizerId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        type: String? = null,
        upcoming: Boolean? = null
    ): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsRetrieve(daysAhead, groupId, organizerId, page, pageSize, type, upcoming) }

    suspend fun getEvent(id: Int): Result<EventDetail> =
        safeApiCall { api.apiV1EventsEventsRetrieve2(id) }

    suspend fun searchEvents(
        endDate: String? = null,
        location: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        q: String? = null,
        startDate: String? = null,
        type: String? = null
    ): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsSearchRetrieve(endDate, location, page, pageSize, q, startDate, type) }

    suspend fun getEventStatistics(id: Int): Result<EventStatistics> =
        safeApiCall { api.apiV1EventsEventsStatisticsRetrieve(id) }

    suspend fun getEventTimeline(endDate: String, startDate: String, includeAttending: Boolean? = null, includeOrganized: Boolean? = null): Result<List<EventTimeline>> =
        safeApiCall { api.apiV1EventsEventsTimelineList(endDate, startDate, includeAttending, includeOrganized) }

    suspend fun getEventsByType(eventType: String, page: Int? = null, pageSize: Int? = null, upcomingOnly: Boolean? = null): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsTypeRetrieve(eventType, page, pageSize, upcomingOnly) }

    suspend fun getUpcomingEvents(
        daysAhead: Int? = null,
        groupId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        type: String? = null,
        userId: Int? = null
    ): Result<PaginatedEventList> =
        safeApiCall { api.apiV1EventsEventsUpcomingRetrieve(daysAhead, groupId, page, pageSize, type, userId) }

    suspend fun updateEvent(id: Int, request: EventUpdateRequest): Result<EventDetail> =
        safeApiCall { api.apiV1EventsEventsUpdate(id, request) }

    suspend fun updateEventAlt(id: Int): Result<EventList> =
        safeApiCall { api.apiV1EventsEventsUpdateUpdate(id) }
}