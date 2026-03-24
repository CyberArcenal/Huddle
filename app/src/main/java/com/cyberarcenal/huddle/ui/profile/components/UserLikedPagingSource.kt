package com.cyberarcenal.huddle.ui.profile.components


import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.UnifiedContentItem
import com.cyberarcenal.huddle.data.repositories.UserContentRepository

class UserLikedPagingSource(
    private val userId: Int?,
    private val userContentRepository: UserContentRepository,
    private val isCurrentUser: Boolean
) : PagingSource<Int, UnifiedContentItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UnifiedContentItem> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            val result = if (isCurrentUser) {
                userContentRepository.getMyLikedItems(page = page, pageSize = pageSize)
            } else {
                userId?.let {
                    userContentRepository.getUserLikedItems(
                        it,
                        page = page,
                        pageSize = pageSize
                    )
                }
                    ?: throw IllegalStateException("userId required for non-current user")
            }

            result.fold(
                onSuccess = { response ->
                    LoadResult.Page(
                        data = response.results,
                        prevKey = if (page > 1) page - 1 else null,
                        nextKey = if (response.hasNext) page + 1 else null
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UnifiedContentItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}