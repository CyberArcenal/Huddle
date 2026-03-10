package com.cyberarcenal.huddle.ui.search

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.PaginatedSearchResult
import com.cyberarcenal.huddle.api.models.SearchResult
import com.cyberarcenal.huddle.data.repositories.users.UsersRepository

class SearchPagingSource(
    private val repository: UsersRepository,
    private val query: String
) : PagingSource<Int, SearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        return try {
            val page = params.key ?: 1
            val result = repository.searchUsers(query, page, params.loadSize)
            result.fold(
                onSuccess = { data: PaginatedSearchResult ->
                    LoadResult.Page(
                        data = data.results,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (data.next == null) null else page + 1
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

    override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}