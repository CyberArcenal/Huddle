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

    override suspend fun initialize(): InitializeAction {
        // Alisin ang timeout para laging i-load ang nasa DB first
        return InitializeAction.SKIP_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, FeedEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
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
                onFailure = { error -> MediatorResult.Error(error) }
            )
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
