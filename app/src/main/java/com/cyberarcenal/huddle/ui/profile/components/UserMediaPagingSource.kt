package com.cyberarcenal.huddle.ui.profile.components

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.UserMediaItem
import com.cyberarcenal.huddle.data.repositories.UserMediaRepository

class UserMediaPagingSource(
    private val userId: Int?,
    private val userMediaRepository: UserMediaRepository,
    private val isCurrentUser: Boolean
) : PagingSource<Int, UserMediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserMediaItem> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            val result = if (isCurrentUser) {
                userMediaRepository.getMyMediaGrid(page = page, pageSize = pageSize)
            } else {
                userId?.let { userMediaRepository.getUserMediaGrid(it, page = page, pageSize = pageSize) }
                    ?: throw IllegalStateException("userId required for non-current user")
            }

            result.fold(
                onSuccess = { response ->
                    LoadResult.Page(
                        data = response.data.results,
                        prevKey = if (page > 1) page - 1 else null,
                        nextKey = if (response.data.hasNext) page + 1 else null
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UserMediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}