package com.cyberarcenal.huddle.ui.search

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.UserSearch
import com.cyberarcenal.huddle.data.repositories.GlobalDedicatedSearchsRepository

class SearchPagingSource(
    private val repository: GlobalDedicatedSearchsRepository,
    private val query: String
) : PagingSource<Int, UserSearch>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserSearch> {
        return try {
            val page = params.key ?: 1
            val result = repository.searchUsers(query, page, params.loadSize)
            result.fold(
                onSuccess = { data ->
                    LoadResult.Page(
                        data = data.results,
                        prevKey = if (data.hasPrev) page - 1 else null,
                        nextKey = if (data.hasNext) page + 1 else null
                    )
                },
                onFailure = { error ->
                    Log.e("SearchPagingSource", "Search failed for query: $query, page: $page", error)
                    LoadResult.Error(error)
                }
            )
        } catch (e: Exception) {
            Log.e("SearchPagingSource", "Unexpected error in search paging", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UserSearch>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}