package com.cyberarcenal.huddle.data.repositories

import android.content.Context
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.local.HuddleDatabase
import com.cyberarcenal.huddle.data.local.entities.StoryEntity
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.emptyList

data class StoryCreateRequestWithMedia(
    val storyType: StoryTypeEnum,
    val content: String? = null,
    val mediaFile: File? = null,
    val expiresInHours: Int = 24,
    val mimeType: String? = null
)

interface StoryCreateApi {
    @Multipart
    @POST("api/v1/stories/stories/")
    suspend fun storiesCreate(
        @Part("story_type") storyType: RequestBody,
        @Part("content") content: RequestBody? = null,
        @Part mediaFile: MultipartBody.Part? = null,
        @Part("mimeTypes") mimeTypes: RequestBody? = null,
        @Part("expires_in_hours") expiresInHours: RequestBody? = null
    ): Response<StoryCreateResponse>
}

class StoriesRepository(context: Context) {
    private val database = HuddleDatabase.getDatabase(context)
    private val storyDao = database.storyDao()
    private val api = ApiService.storiesApi
    private val createStoryApi: StoryCreateApi = ApiService.storyCreateApi

    fun observeStories(type: String): Flow<List<StoryFeed>> =
        storyDao.observeStories(type).map { entities ->
            entities.map { it.rawData }
        }

    suspend fun fetchAndCacheStories(type: String = "FOLLOWING"): Result<Unit> = safeApiCall {
        val response = when (type) {
            "FOLLOWING" -> api.apiV1StoriesStoriesFollowingRetrieve(null)
            else -> api.apiV1StoriesStoriesFeedRetrieve(includeOwn = true)
        }
        
        if (response.isSuccessful) {
            val stories = response.body()?.data?.feed ?: emptyList()
            val entities = stories.mapIndexed { index, storyFeed ->
                StoryEntity(
                    storyType = type,
                    position = index,
                    rawData = storyFeed
                )
            }
            storyDao.refreshStories(type, entities)
        }
        response
    }.map { Unit }

    suspend fun createStory(request: StoryCreateRequestWithMedia): Result<StoryCreateResponse> =
        safeApiCall {
            val mediaPart = request.mediaFile?.asRequestBody(
                (request.mimeType ?: "image/*").toMediaTypeOrNull()
            )?.let {
                MultipartBody.Part.createFormData("media_file", request.mediaFile.name, it)
            }

            createStoryApi.storiesCreate(
                storyType = request.storyType.value.toRequestBody("text/plain".toMediaTypeOrNull()),
                content = request.content?.toRequestBody("text/plain".toMediaTypeOrNull()),
                mediaFile = mediaPart,
                mimeTypes = request.mimeType?.toRequestBody("text/plain".toMediaTypeOrNull()),
                expiresInHours = request.expiresInHours.toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())
            )
        }

    suspend fun getStoryLists(page: Int?, pageSize: Int?): Result<StoryListResponse> =
        safeApiCall { api.apiV1StoriesStoriesRetrieve(page, pageSize) }

    suspend fun getStoryFeed(
        offset: Int = 0,
        limit: Int = 10,
        includeOwn: Boolean = true,
        limitPerUser: Int = 3
    ): Result<StoryFeedListResponse> =
        safeApiCall {
            api.apiV1StoriesStoriesFeedRetrieve(
                includeOwn = includeOwn,
                limitPerUser = limitPerUser,
                offset = offset,
                limit = limit
            )
        }

    suspend fun getMyStories(
        page: Int? = null,
        pageSize: Int? = null,
        includeExpired: Boolean? = null
    ): Result<StoryListResponse> =
        safeApiCall { api.apiV1StoriesMeStoriesRetrieve(includeExpired, page, pageSize) }

    suspend fun getUserStories(
        userId: Int,
        page: Int? = null,
        pageSize: Int? = null,
        includeExpired: Boolean? = null
    ): Result<StoryListResponse> =
        safeApiCall { api.apiV1StoriesUsersStoriesRetrieve(userId, includeExpired, page, pageSize) }

    suspend fun getPopularStories(
        hours: Int? = null,
        limit: Int? = null
    ): Result<PopularStoriesResponse> =
        safeApiCall { api.apiV1StoriesStoriesPopularRetrieve(hours, limit) }

    suspend fun getStoryRecommendations(limit: Int? = null): Result<StoryRecommendationsResponse> =
        safeApiCall { api.apiV1StoriesStoriesRecommendationsRetrieve(limit) }

    suspend fun getStoryStats(): Result<StoryStatsResponse> =
        safeApiCall { api.apiV1StoriesStoriesStatsRetrieve() }

    suspend fun deactivateStory(storyId: Int): Result<StoryDeactivateResponse> =
        safeApiCall { api.apiV1StoriesStoriesDeactivateCreate(storyId) }

    suspend fun deleteStoryPermanent(storyId: Int): Result<StoryDeleteResponse> =
        safeApiCall { api.apiV1StoriesStoriesDestroy(storyId) }

    suspend fun extendStory(storyId: Int, hours: Int? = null): Result<StoryExtendResponse> =
        safeApiCall {
            api.apiV1StoriesStoriesExtendCreate(storyId, ExtendStoryInputRequest(hours))
        }

    suspend fun getFollowingStories(limit: Int? = null): Result<StoryFeedListResponse> =
        safeApiCall { api.apiV1StoriesStoriesFollowingRetrieve(limit) }

    suspend fun getStoryHighlights(
        days: Int? = null,
        limit: Int? = null
    ): Result<StoryHighlightsResponse> =
        safeApiCall { api.apiV1StoriesStoriesHighlightsRetrieve(days, limit) }

    suspend fun getStory(storyId: Int): Result<StoryDetailResponse> =
        safeApiCall { api.apiV1StoriesStoriesRetrieve2(storyId) }

    suspend fun getStoriesByType(
        storyType: String,
        activeOnly: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<StoryListResponse> =
        safeApiCall { api.apiV1StoriesStoriesTypeRetrieve(storyType, activeOnly, page, pageSize) }

    suspend fun updateStory(
        storyId: Int,
        request: StoryUpdateRequest? = null
    ): Result<StoryUpdateResponse> =
        safeApiCall { api.apiV1StoriesStoriesUpdate(storyId, request) }

    suspend fun getStoryViewCount(storyId: Int): Result<StoryViewCountResponse> =
        safeApiCall { api.apiV1StoriesStoriesViewCountRetrieve(storyId) }

    suspend fun getHighlights(): Result<StoryHighlightListResponse> =
        safeApiCall { api.apiV1StoriesHighlightsRetrieve() }

    suspend fun createHighlight(request: StoryHighlightCreateRequest): Result<StoryHighlightCreateResponse> =
        safeApiCall { api.apiV1StoriesHighlightsCreate(request) }

    suspend fun updateHighlight(
        id: Int,
        request: StoryHighlightUpdateRequest? = null
    ): Result<StoryHighlightUpdateResponse> =
        safeApiCall { api.apiV1StoriesHighlightsUpdate(id, request) }

    suspend fun deleteHighlight(highlightId: Int): Result<StoryHighlightDeleteResponse> =
        safeApiCall { api.apiV1StoriesHighlightsDestroy(highlightId) }

    suspend fun addStoriesToHighlight(
        highlightId: Int,
        body: StoryHighlightAddStoriesRequest
    ): Result<StoryHighlightAddStoriesResponse> =
        safeApiCall { api.apiV1StoriesHighlightsAddStoriesCreate(highlightId, body) }

    suspend fun removeStoriesFromHighlight(
        highlightId: Int,
        body: StoryHighlightRemoveStoriesRequest
    ): Result<StoryHighlightRemoveStoriesResponse> =
        safeApiCall { api.apiV1StoriesHighlightsRemoveStoriesCreate(highlightId, body) }

    suspend fun setHighlightCover(
        highlightId: Int,
        request: StoryHighlightSetCoverRequest
    ): Result<StoryHighlightSetCoverResponse> =
        safeApiCall { api.apiV1StoriesHighlightsSetCoverCreate(highlightId, request) }

    suspend fun getHighlight(highlightId: Int): Result<StoryHighlightDetailResponse> =
        safeApiCall { api.apiV1StoriesHighlightsRetrieve2(highlightId) }

    suspend fun getPublicHighlight(userId: Int): Result<StoryHighlightListResponse> =
        safeApiCall { api.apiV1StoriesHighlightsRetrieve(userId) }

}
