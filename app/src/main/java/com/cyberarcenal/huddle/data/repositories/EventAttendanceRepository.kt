package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class EventAttendanceRepository {
    private val api = ApiService.eventAttendanceApi

    suspend fun removeAttendance(eventId: Int): Result<EventAttendanceDeleteResponse> =
        safeApiCall { api.apiV1EventsEventsAttendanceDestroy(eventId) }

    suspend fun removeAttendanceForUser(eventId: Int, userId: Int): Result<EventAttendanceDeleteResponse> =
        safeApiCall { api.apiV1EventsEventsAttendanceDestroy2(eventId, userId) }

    suspend fun partialUpdateAttendance(
        eventId: Int,
        request: PatchedEventAttendanceUpdateRequest? = null
    ): Result<EventAttendanceUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsAttendancePartialUpdate(eventId, request) }

    suspend fun partialUpdateAttendanceForUser(
        eventId: Int,
        userId: Int,
        request: PatchedEventAttendanceUpdateRequest? = null
    ): Result<EventAttendanceUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsAttendancePartialUpdate2(eventId, userId, request) }

    suspend fun getAttendance(eventId: Int): Result<EventAttendanceDetailResponse> =
        safeApiCall { api.apiV1EventsEventsAttendanceRetrieve(eventId) }

    suspend fun getAttendanceForUser(eventId: Int, userId: Int): Result<EventAttendanceDetailResponse> =
        safeApiCall { api.apiV1EventsEventsAttendanceRetrieve2(eventId, userId) }

    suspend fun updateAttendance(
        eventId: Int,
        request: EventAttendanceUpdateRequest? = null
    ): Result<EventAttendanceUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsAttendanceUpdate(eventId, request) }

    suspend fun updateAttendanceForUser(
        eventId: Int,
        userId: Int,
        request: EventAttendanceUpdateRequest? = null
    ): Result<EventAttendanceUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsAttendanceUpdate2(eventId, userId, request) }

    suspend fun rsvp(eventId: Int, request: EventAttendanceCreateRequest): Result<EventAttendanceCreateResponse> =
        safeApiCall { api.apiV1EventsEventsAttendeesCreate(eventId, request) }

    suspend fun getMutualAttendees(eventId: Int): Result<MutualAttendeesResponse> =
        safeApiCall { api.apiV1EventsEventsAttendeesMutualRetrieve(eventId) }

    suspend fun sendReminders(
        eventId: Int,
        request: SendRemindersInputRequest? = null
    ): Result<SendRemindersResponse> =
        safeApiCall { api.apiV1EventsEventsAttendeesRemindersCreate(eventId, request) }

    suspend fun getAttendees(
        eventId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        status: String? = null
    ): Result<EventAttendanceListResponse> =
        safeApiCall { api.apiV1EventsEventsAttendeesRetrieve(eventId, page, pageSize, status) }

    suspend fun getAttendanceTrend(
        eventId: Int,
        hoursBefore: Int? = null
    ): Result<AttendanceTrendResponse> =
        safeApiCall { api.apiV1EventsEventsAttendeesTrendRetrieve(eventId, hoursBefore) }

    suspend fun rsvpAlt(eventId: Int, request: RSVPInputRequest? = null): Result<EventAttendanceCreateResponse> =
        safeApiCall { api.apiV1EventsEventsRsvpCreate(eventId, request) }

    suspend fun updateAttendanceStatus(
        eventId: Int,
        request: PatchedUpdateStatusInputRequest? = null
    ): Result<EventAttendanceUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsRsvpStatusPartialUpdate(eventId, request) }

    suspend fun getUserEvents(
        endDate: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        startDate: String? = null,
        status: String? = null,
        upcomingOnly: Boolean? = null,
        userId: Int? = null
    ): Result<UserEventsResponse> =
        safeApiCall { api.apiV1EventsUserEventsRetrieve(endDate, page, pageSize, startDate, status, upcomingOnly, userId) }

    suspend fun getUserEventsForUser(
        userId: Int,
        endDate: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        startDate: String? = null,
        status: String? = null,
        upcomingOnly: Boolean? = null,
        userId2: Int? = null
    ): Result<UserEventsResponse> =
        safeApiCall { api.apiV1EventsUserEventsRetrieve2(userId, endDate, page, pageSize, startDate, status, upcomingOnly, userId2) }

    suspend fun getUserAttendanceStatistics(userId: Int? = null): Result<UserAttendanceStatisticsResponse> =
        safeApiCall { api.apiV1EventsUserEventsStatisticsRetrieve(userId) }

    suspend fun getUserAttendanceStatisticsForUser(
        userId: Int,
        userId2: Int? = null
    ): Result<UserAttendanceStatisticsResponse> =
        safeApiCall { api.apiV1EventsUserEventsStatisticsRetrieve2(userId, userId2) }
}