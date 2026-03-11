package com.cyberarcenal.huddle.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.Conversation
import com.cyberarcenal.huddle.data.repositories.messaging.MessagingRepository
import kotlinx.coroutines.flow.Flow

class ConversationsViewModel(
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    val conversationsFlow: Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        ConversationsPagingSource(messagingRepository)
    }.flow.cachedIn(viewModelScope)
}

class ConversationsPagingSource(
    private val repository: MessagingRepository
) : androidx.paging.PagingSource<Int, Conversation>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Conversation> {
        return try {
            val page = params.key ?: 1
            val result = repository.getConversations(page = page, pageSize = params.loadSize)
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

    override fun getRefreshKey(state: PagingState<Int, Conversation>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}