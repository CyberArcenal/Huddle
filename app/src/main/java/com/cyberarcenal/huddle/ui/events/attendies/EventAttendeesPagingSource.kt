package com.cyberarcenal.huddle.ui.events.attendies

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.EventAttendanceWithUser
import com.cyberarcenal.huddle.data.repositories.EventAttendanceRepository

class EventAttendeesPagingSource(
    private val repository: EventAttendanceRepository,
    private val eventId: Int,
    private val searchQuery: String,
    private val personality: String?,
    private val sort: String,
    private val friendsOnly: Boolean
) : PagingSource<Int, EventAttendanceWithUser>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EventAttendanceWithUser> {
        return try {
            val page = params.key ?: 1
            val response = repository.getAttendeesFiltered(
                eventId = eventId,
                page = page,
                pageSize = params.loadSize,
                search = searchQuery.takeIf { it.isNotBlank() },
                personality = personality,
                sort = sort,
                friendsOnly = friendsOnly
            )

            if (response.isSuccess) {
                val data = response.getOrThrow()
                val attendees = data.data.results ?: emptyList()
                val nextKey = if (data.data.hasNext) page + 1 else null
                LoadResult.Page(
                    data = attendees,
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

    override fun getRefreshKey(state: PagingState<Int, EventAttendanceWithUser>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}