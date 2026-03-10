package com.cyberarcenal.huddle.data.repositories.stories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class StoriesRepository {
    private val api = ApiService.v1Api

    // ========== STORIES ==========

    /**
     * Get a paginated list of active stories (including those of followed users and public stories).
     */
    suspend fun getStories(
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedStory> = safeApiCall {
        api.v1StoriesStoriesRetrieve(page, pageSize)
    }

    /**
     * Create a new story. The story will be active for 24 hours.
     */
    suspend fun createStory(storyCreate: StoryCreate): Result<Story> = safeApiCall {
        api.v1StoriesStoriesCreate(storyCreate)
    }

    /**
     * Get a single story by ID.
     */
    suspend fun getStory(storyId: Int): Result<Story> = safeApiCall {
        api.v1StoriesStoriesRetrieve2(storyId)
    }

    /**
     * Update a story (e.g., change caption). Only the owner can update.
     */
    suspend fun updateStory(storyId: Int, storyUpdate: StoryUpdate): Result<Story> = safeApiCall {
        api.v1StoriesStoriesUpdate(storyId, storyUpdate)
    }

    /**
     * Permanently delete a story. Only the owner can delete.
     */
    suspend fun deleteStory(storyId: Int): Result<V1StoriesStoriesDestroy200Response> = safeApiCall {
        api.v1StoriesStoriesDestroy(storyId)
    }

    /**
     * Deactivate a story (soft delete). Only the owner can deactivate.
     */
    suspend fun deactivateStory(storyId: Int): Result<V1StoriesStoriesDestroy200Response> = safeApiCall {
        api.v1StoriesStoriesDeactivateCreate(storyId)
    }

    /**
     * Extend the life of an active story by a given number of hours. Only the owner can extend.
     */
    suspend fun extendStory(storyId: Int, additionalHours: Int): Result<V1StoriesStoriesDestroy200Response> {
        val body = ExtendStoryInput(additionalHours = additionalHours)
        return safeApiCall { api.v1StoriesStoriesExtendCreate(storyId, body) }
    }

    // ========== STORY FEEDS ==========

    /**
     * Get a personalized story feed grouped by user.
     */
    suspend fun getStoryFeed(
        includeOwn: Boolean? = null,
        limitPerUser: Int? = null,
        maxUsers: Int? = null
    ): Result<List<StoryFeed>> = safeApiCall {
        api.v1StoriesStoriesFeedList(includeOwn, limitPerUser, maxUsers)
    }

    /**
     * Get stories from users followed by the current user, grouped by user.
     */
    suspend fun getFollowingStories(limit: Int? = null): Result<List<StoryFeed>> = safeApiCall {
        api.v1StoriesStoriesFollowingList(limit)
    }

    /**
     * Get stories posted by a specific user (paginated).
     */
    suspend fun getUserStories(
        userId: Int,
        includeExpired: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedStory> = safeApiCall {
        api.v1StoriesUsersStoriesRetrieve(userId, includeExpired, page, pageSize)
    }

    /**
     * Get stories posted by the current user (paginated).
     */
    suspend fun getMyStories(
        includeExpired: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedStory> = safeApiCall {
        api.v1StoriesMeStoriesRetrieve(includeExpired, page, pageSize)
    }

    // ========== STORY INTERACTIONS ==========

    /**
     * Record that the current user has viewed a story.
     */
    suspend fun markStoryViewed(storyId: Int): Result<StoryView> {
        val create = StoryViewCreate(storyId = storyId)
        return safeApiCall { api.v1StoriesStoriesViewCreate(storyId, create) }
    }

    /**
     * Get total view count and unique viewers for a story.
     */
    suspend fun getStoryViewCount(storyId: Int): Result<StoryViewCount> = safeApiCall {
        api.v1StoriesStoriesViewCountRetrieve(storyId)
    }

    /**
     * Get a paginated list of users who viewed a story. Only the story owner can access.
     */
    suspend fun getStoryViewers(
        storyId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedStoryView> = safeApiCall {
        api.v1StoriesStoriesViewsRetrieve(storyId, page, pageSize)
    }

    /**
     * Get a list of recent viewers of a story. Only the story owner can access.
     */
    suspend fun getRecentViewers(
        storyId: Int,
        hours: Int? = null,
        limit: Int? = null
    ): Result<List<StoryRecentViewer>> = safeApiCall {
        api.v1StoriesStoriesRecentViewersList(storyId, hours, limit)
    }

    // ========== STORY STATISTICS & HIGHLIGHTS ==========

    /**
     * Get statistics about the current user's stories (total, views, etc.).
     */
    suspend fun getMyStoryStats(): Result<StoryStats> = safeApiCall {
        api.v1StoriesStoriesStatsRetrieve()
    }

    /**
     * Get statistics about the current user's story viewing habits.
     */
    suspend fun getMyViewingStats(): Result<Any> = safeApiCall {
        api.v1StoriesStoriesViewStatsRetrieve()
    }

    /**
     * Get highlighted stories (most viewed) for the current user.
     */
    suspend fun getStoryHighlights(
        days: Int? = null,
        limit: Int? = null
    ): Result<List<StoryHighlight>> = safeApiCall {
        api.v1StoriesStoriesHighlightsList(days, limit)
    }

    /**
     * Get the most viewed stories in the last N hours.
     */
    suspend fun getPopularStories(
        hours: Int? = null,
        limit: Int? = null
    ): Result<List<Any>> = safeApiCall {
        api.v1StoriesStoriesPopularRetrieve(hours, limit)
    }

    /**
     * Get personalized story recommendations for the current user.
     */
    suspend fun getStoryRecommendations(limit: Int? = null): Result<List<StoryRecommendation>> = safeApiCall {
        api.v1StoriesStoriesRecommendationsList(limit)
    }

    /**
     * Get mutual story viewing data between the current user and another user.
     */
    suspend fun getMutualViews(otherUserId: Int): Result<Any> = safeApiCall {
        api.v1StoriesUsersMutualViewsRetrieve(otherUserId)
    }

    // ========== ADMIN ==========

    /**
     * Admin endpoint to clean up expired stories (deactivate or delete).
     */
    suspend fun adminCleanupStories(deactivateOnly: Boolean? = null): Result<StoryCleanupResponse> {
        val body = CleanupStoriesInput(deactivateOnly = deactivateOnly)
        return safeApiCall { api.v1StoriesAdminStoriesCleanupCreate(body) }
    }
}