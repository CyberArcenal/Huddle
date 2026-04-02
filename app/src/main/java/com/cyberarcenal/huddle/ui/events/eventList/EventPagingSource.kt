package com.cyberarcenal.huddle.ui.events.eventList

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.data.repositories.EventRepository

class EventPagingSource(
    private val repo: EventRepository,
    private val isUpcoming: Boolean = true,
    private val userId: Int? = null,
    private val myEvents: Boolean = false,
    private val organizedByMe: Boolean = false
) : PagingSource<Int, EventList>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EventList> {
        return try {
            val page = params.key ?: 1
            val response = when {
                organizedByMe -> repo.getOrganizedEvents(page = page, pageSize = params.loadSize, upcomingOnly = isUpcoming)
                myEvents -> repo.getEvents(
                    page = page,
                    pageSize = params.loadSize,
                    upcoming = isUpcoming,
                    userId = userId // API supports filtering by user attending
                )
                else -> repo.getUpcomingEvents(
                    page = page,
                    pageSize = params.loadSize,
                    daysAhead = 30
                )
            }

            if (response.isSuccess) {
                val data = response.getOrThrow()
                val events = data.data.results ?: emptyList()
                val nextKey = if (data.data.hasNext) page + 1 else null
                LoadResult.Page(
                    data = events,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(Exception(response.exceptionOrNull()?.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, EventList>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}