package com.cyberarcenal.huddle.ui.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.FeedRow
import com.cyberarcenal.huddle.data.repositories.FeedRepository

enum class FeedType { HOME, DISCOVER, FRIENDS, FOLLOWING, GROUPS }

class FeedPagingSource(
    private val repository: FeedRepository,
    private val feedType: FeedType
) : PagingSource<Int, FeedRow>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedRow> {
        val page = params.key ?: 1
        return try {
            val backendFeedType = when (feedType) {
                FeedType.HOME -> "home"
                FeedType.DISCOVER -> "discover"
                FeedType.FRIENDS -> "friends_posts"
                FeedType.FOLLOWING -> "following_posts"
                FeedType.GROUPS -> "group_posts"
            }

            val result = repository.getFeed(
                feedType = backendFeedType,
                page = page,
                pageSize = params.loadSize
            )

            result.fold(
                onSuccess = { response ->
                    // Ang 'results' ay listahan ng mga Rows (e.g. Row ng Posts, Row ng Stories)
                    val data = response.results ?: emptyList()

                    LoadResult.Page(
                        data = data,
                        prevKey = if (page == 1) null else page - 1,
                        // Gamitin ang 'hasNext' property mula sa iyong JSON
                        nextKey = if (response.hasNext) page + 1 else null
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, FeedRow>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}