package com.cyberarcenal.huddle.ui.profile

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository

class ProfilePagingSource(
    private val userId: Int,
    private val postRepository: UserPostsRepository
) : androidx.paging.PagingSource<Int, PostFeed>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostFeed> {
        return try {
            val page = params.key ?: 1
            val result = postRepository.getPosts(userId = userId, page = page, pageSize = params.loadSize)
            result.fold(
                onSuccess = { data ->
                    LoadResult.Page(
                        data = data.results,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (data.next == null) null else page + 1
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PostFeed>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}