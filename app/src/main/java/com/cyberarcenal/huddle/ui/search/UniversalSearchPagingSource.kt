package com.cyberarcenal.huddle.ui.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.data.repositories.DedicatedSearchRepositories
import com.cyberarcenal.huddle.data.repositories.UserSearchRepository

class UniversalSearchPagingSource(
    private val repository: UserSearchRepository,
    private val dedicatedSearchRepositories: DedicatedSearchRepositories,
    private val query: String,
    private val category: SearchCategory
) : PagingSource<Int, Any>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Any> {
        val page = params.key ?: 1
        return try {
            val result = when (category) {
                SearchCategory.USERS -> repository.searchUsers(query, page = page, pageSize = params.loadSize)
                SearchCategory.GROUPS -> dedicatedSearchRepositories.searchGroups(query, page = page, pageSize = params.loadSize)
                SearchCategory.POSTS -> dedicatedSearchRepositories.searchPosts(query, page = page, pageSize = params.loadSize)
                SearchCategory.EVENTS -> dedicatedSearchRepositories.searchEvents(query, page = page, pageSize = params.loadSize)
            }

            result.fold(
                onSuccess = { data ->
                    // Gagamit tayo ng reflection para makuha ang 'results' at 'next' mula sa magkakaibang models
                    val items = data.javaClass.getMethod("getResults").invoke(data) as List<Any>
                    val next = data.javaClass.getMethod("getNext").invoke(data)

                    LoadResult.Page(
                        data = items,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (next == null) null else page + 1
                    )
                },
                onFailure = { error -> LoadResult.Error(error) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Any>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
