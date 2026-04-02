package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface EventCreateApi {
    /**
     * POST api/v1/events/events/
     *
     * Create a new event.
     * Responses:
     *  - 202:
     *  - 400:
     *
     * @param title
     * @param description
     * @param location
     * @param startTime
     * @param endTime
     * @param eventType  (optional)
     * @param group  (optional)
     * @param maxAttendees  (optional)
     * @param media  (optional)
     * @param clientId  (optional)
     * @return [EventCreateResponse]
     */
    @Multipart
    @POST("api/v1/events/events/")
    suspend fun eventsCreate(
        @Part("title") title: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part("location") location: okhttp3.RequestBody,
        @Part("start_time") startTime: okhttp3.RequestBody,
        @Part("end_time") endTime: okhttp3.RequestBody,
        @Part("event_type") eventType: okhttp3.RequestBody? = null,
        @Part("group") group: okhttp3.RequestBody? = null,
        @Part("max_attendees") maxAttendees: okhttp3.RequestBody? = null,
        @Part media: List<MultipartBody.Part>? = null,
        @Part("client_id") clientId: okhttp3.RequestBody? = null
    ): Response<EventCreateResponse>

}

class EventRepository {
    private val api = ApiService.eventApi;
    private val createApi = ApiService.eventCreateApi;

    suspend fun createEvent(
        title: String,
        description: String,
        location: String,
        startTime: String,
        endTime: String,
        eventType: EventType8c2Enum? = null,
        group: Int? = null,
        maxAttendees: Long? = null,
        media: List<MultipartBody.Part>? = null,
        clientId: String? = null

    ): Result<EventCreateResponse> =
        safeApiCall {
            val textType = "text/plain".toMediaTypeOrNull()
            createApi.eventsCreate(
                title = title.toRequestBody(textType),
                description = description.toRequestBody(textType),
                location = location.toRequestBody(textType),
                startTime = startTime.toRequestBody(textType),
                endTime = endTime.toRequestBody(textType),
                eventType = eventType?.value?.toRequestBody(textType),
                group = group?.toString()?.toRequestBody(textType),
                maxAttendees = maxAttendees?.toString()?.toRequestBody(textType),
                media = media,
                clientId = clientId?.toRequestBody(textType)
            )
        }

    suspend fun checkCreateStatus(id: Int): Result<EventStatusResponse> = safeApiCall {
        api.apiV1EventsStatusRetrieve(id)
    }

    suspend fun createEventAlt(request: EventCreateRequest): Result<EventCreateResponse> =
        safeApiCall { api.apiV1EventsEventsCreateCreate(request) }

    suspend fun deleteEvent(id: Int): Result<EventDeleteResponse> =
        safeApiCall { api.apiV1EventsEventsDeleteDestroy(id) }

    suspend fun deleteEventAlt(id: Int): Result<EventDeleteResponse> =
        safeApiCall { api.apiV1EventsEventsDestroy(id) }

    suspend fun getFeaturedEvents(
        daysAhead: Int? = null,
        limit: Int? = null,
        minAttendees: Int? = null
    ): Result<FeaturedEventsResponse> =
        safeApiCall { api.apiV1EventsEventsFeaturedRetrieve(daysAhead, limit, minAttendees) }

    suspend fun getGroupEvents(
        groupId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<EventListResponse> =
        safeApiCall { api.apiV1EventsEventsGroupRetrieve(groupId, page, pageSize, upcomingOnly) }

    suspend fun getOrganizedEvents(
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null,
        userId: Int? = null
    ): Result<EventListResponse> =
        safeApiCall { api.apiV1EventsEventsOrganizedRetrieve(page, pageSize, upcomingOnly, userId) }

    suspend fun getOrganizedEventsForUser(
        userId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null,
        userId2: Int? = null
    ): Result<EventListResponse> =
        safeApiCall {
            api.apiV1EventsEventsOrganizedRetrieve2(
                userId,
                page,
                pageSize,
                upcomingOnly,
                userId2
            )
        }

    suspend fun partialUpdateEvent(
        id: Int,
        request: PatchedEventUpdateRequest? = null
    ): Result<EventUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsPartialUpdate(id, request) }

    suspend fun getPastEvents(
        daysBack: Int? = null,
        groupId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        userId: Int? = null
    ): Result<EventListResponse> =
        safeApiCall { api.apiV1EventsEventsPastRetrieve(daysBack, groupId, page, pageSize, userId) }

    suspend fun getRecommendedEvents(limit: Int? = null): Result<FeaturedEventsResponse> =
        safeApiCall { api.apiV1EventsEventsRecommendedRetrieve(limit) }

    suspend fun getEvents(
        daysAhead: Int? = null,
        groupId: Int? = null,
        organizerId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        type: String? = null,
        upcoming: Boolean? = null,
        userId: Int?
    ): Result<EventListResponse> =
        safeApiCall {
            api.apiV1EventsEventsRetrieve(
                daysAhead,
                groupId,
                organizerId,
                page,
                pageSize,
                type,
                upcoming
            )
        }

    suspend fun getEvent(id: Int): Result<EventDetailResponse> =
        safeApiCall { api.apiV1EventsEventsRetrieve2(id) }

    suspend fun searchEvents(
        endDate: String? = null,
        location: String? = null,
        page: Int? = null,
        pageSize: Int? = null,
        q: String? = null,
        startDate: String? = null,
        type: String? = null
    ): Result<EventListResponse> =
        safeApiCall {
            api.apiV1EventsEventsSearchRetrieve(
                endDate,
                location,
                page,
                pageSize,
                q,
                startDate,
                type
            )
        }

    suspend fun getEventStatistics(id: Int): Result<EventStatisticsResponse> =
        safeApiCall { api.apiV1EventsEventsStatisticsRetrieve(id) }

    suspend fun getEventTimeline(
        endDate: String,
        startDate: String,
        includeAttending: Boolean? = null,
        includeOrganized: Boolean? = null
    ): Result<EventTimelineResponse> =
        safeApiCall {
            api.apiV1EventsEventsTimelineRetrieve(
                endDate,
                startDate,
                includeAttending,
                includeOrganized
            )
        }

    suspend fun getEventsByType(
        eventType: String,
        page: Int? = null,
        pageSize: Int? = null,
        upcomingOnly: Boolean? = null
    ): Result<EventListResponse> =
        safeApiCall { api.apiV1EventsEventsTypeRetrieve(eventType, page, pageSize, upcomingOnly) }

    suspend fun getUpcomingEvents(
        daysAhead: Int? = null,
        groupId: Int? = null,
        page: Int? = null,
        pageSize: Int? = null,
        type: String? = null,
        userId: Int? = null
    ): Result<EventListResponse> =
        safeApiCall {
            api.apiV1EventsEventsUpcomingRetrieve(
                daysAhead,
                groupId,
                page,
                pageSize,
                type,
                userId
            )
        }

    suspend fun updateEvent(id: Int, request: EventUpdateRequest): Result<EventUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsUpdate(id, request) }

    suspend fun updateEventAlt(id: Int, request: EventUpdateRequest): Result<EventUpdateResponse> =
        safeApiCall { api.apiV1EventsEventsUpdateUpdate(id, request) }




}