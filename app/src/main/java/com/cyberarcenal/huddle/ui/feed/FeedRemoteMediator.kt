// data/remote/FeedRemoteMediator.kt
package com.cyberarcenal.huddle.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.cyberarcenal.huddle.data.local.HuddleDatabase
import com.cyberarcenal.huddle.data.local.entities.FeedEntity
import com.cyberarcenal.huddle.data.repositories.FeedRepository


@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val feedType: String,
    private val repository: FeedRepository,
    private val database: HuddleDatabase
) : RemoteMediator<Int, FeedEntity>() {

    private val remoteKeysDao = database.remoteKeysDao()
    private val feedDao = database.feedDao()
    private val cacheTimeout = 3600000L // 1 hour

    override suspend fun initialize(): InitializeAction {
        val remoteKey = remoteKeysDao.remoteKeysByFeedType(feedType)
        val isValid = remoteKey != null &&
                System.currentTimeMillis() - remoteKey.lastUpdated < cacheTimeout
        return if (isValid) InitializeAction.SKIP_INITIAL_REFRESH
        else InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, FeedEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                // ← Ito ang nagpapagawa ng "like other apps" behavior
                feedDao.clearFeed(feedType)
                remoteKeysDao.deleteByFeedType(feedType)
                1
            }
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = remoteKeysDao.remoteKeysByFeedType(feedType)
                remoteKey?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val result = repository.fetchAndCachePage(feedType, page, state.config.pageSize)
            result.fold(
                onSuccess = {
                    val remoteKey = remoteKeysDao.remoteKeysByFeedType(feedType)
                    val endOfPaginationReached = remoteKey?.nextKey == null
                    MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
                },
                onFailure = { MediatorResult.Error(it) }
            )
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}