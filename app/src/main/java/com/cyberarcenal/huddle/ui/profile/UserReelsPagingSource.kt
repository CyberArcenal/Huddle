package com.cyberarcenal.huddle.ui.profile

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.data.repositories.ReelsRepository

class UserReelsPagingSource(
    private val userId: Int?,
    private val reelsRepository: ReelsRepository
) : PagingSource<Int, ReelDisplay>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReelDisplay> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            val result = reelsRepository.getReels(page = page, pageSize = pageSize, userId = userId)

            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        val pagination = response.data?.pagination
                        LoadResult.Page(
                            data = pagination?.results ?: emptyList(),
                            prevKey = if (page > 1) page - 1 else null,
                            nextKey = if (pagination?.hasNext == true) page + 1 else null
                        )
                    } else {
                        LoadResult.Page(
                            data = emptyList(),
                            prevKey = if (page > 1) page - 1 else null,
                            nextKey = null
                        )
                    }
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ReelDisplay>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
