package com.cyberarcenal.huddle.data.repositories

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.cyberarcenal.huddle.api.models.FeedResponse
import com.cyberarcenal.huddle.api.models.UnifiedContentItem
import com.cyberarcenal.huddle.data.local.HuddleDatabase
import com.cyberarcenal.huddle.data.local.entities.FeedEntity
import com.cyberarcenal.huddle.data.local.entities.RemoteKeys
import com.cyberarcenal.huddle.data.remote.FeedRemoteMediator
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FeedRepository(
    context: Context   // ← optional database para sa caching
) {
    private val database = context.let { HuddleDatabase.getDatabase(it) }
    // Original API instance (hindi binago)
    private val api = ApiService.feedApi

    // ========== EXISTING NETWORK METHOD (gumagana pa rin) ==========
    suspend fun getFeed(
        feedType: String? = "home",
        page: Int? = null,
        pageSize: Int? = null,
        postsPreview: Int? = null,
        sharesPreview: Int? = null
    ): Result<FeedResponse> =
        safeApiCall {
            api.apiV1FeedFeedRetrieve(feedType, page, pageSize, postsPreview, sharesPreview)
        }


    // 2. Pager na may RemoteMediator (para sa PagingData)
    @OptIn(ExperimentalPagingApi::class)
    fun getPagedFeed(feedType: String): Flow<PagingData<UnifiedContentItem>> {
        if (database == null) {
            return flowOf(PagingData.empty())
        }

        return Pager(
            config = PagingConfig(
                pageSize = 15,
                enablePlaceholders = false,
                initialLoadSize = 30,
                prefetchDistance = 3
            ),
            remoteMediator = FeedRemoteMediator(feedType, this, database),
            pagingSourceFactory = {
                // Room PagingSource (FeedEntity)
                database.feedDao().getPagingSource(feedType)
            }
        ).flow
            // ← Dito na natin i-convert ang FeedEntity → UnifiedContentItem
            .map { pagingData: PagingData<FeedEntity> ->
                pagingData.map { entity ->
                    entity.rawData  // UnifiedContentItem
                }
            }
    }


    // 3. Fetch at i-cache ang isang page (ginagamit ng RemoteMediator)
    suspend fun fetchAndCachePage(feedType: String, page: Int, pageSize: Int): Result<Unit> {
        if (database == null) {
            return Result.failure(IllegalStateException("Database not available"))
        }
        val feedDao = database.feedDao()
        val remoteKeysDao = database.remoteKeysDao()

        return try {
            val response = getFeed(feedType.lowercase(), page, pageSize)
            if (response.isSuccess) {
                val feedResponse = response.getOrNull()!!
                if (feedResponse.status) {
                    val items = feedResponse.data?.results ?: emptyList()
                    val feedEntities = items.mapIndexed { index, item ->
                        FeedEntity(
                            feedType = feedType,
                            page = page,
                            position = index,
                            rawData = item,
                            cachedAt = System.currentTimeMillis()
                        )
                    }
                    feedDao.refreshPage(feedType, page, feedEntities)

                    val hasNext = feedResponse.data?.hasNext ?: false
                    val nextKey = if (hasNext) page + 1 else null
                    remoteKeysDao.insertOrReplace(
                        RemoteKeys(
                            feedType = feedType,
                            nextKey = nextKey,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(feedResponse.message))
                }
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("Network error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Kunin ang isang page mula sa cache (ginagamit ng CachedFeedPagingSource)
    suspend fun getCachedPage(feedType: String, page: Int): List<FeedEntity> {
        return if (database == null) {
            emptyList()
        } else {
            database.feedDao().getFeedPage(feedType, page)
        }
    }
}