package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionCount
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionDisplay
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.repositories.ReactionsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReactionListState(val contentType: String, val objectId: Int)

class ReactionManager(
    private val reactionRepository: ReactionsRepository,
    private val viewModelScope: CoroutineScope
) {
    private val _reactionEvents = MutableSharedFlow<ReactionResult>()
    val reactionEvents: SharedFlow<ReactionResult> = _reactionEvents.asSharedFlow()

    private val _reactionListState = MutableStateFlow<ReactionListState?>(null)
    val reactionListState: StateFlow<ReactionListState?> = _reactionListState.asStateFlow()

    private val _reactions = MutableStateFlow<List<ReactionDisplay>>(emptyList<ReactionDisplay>())
    val reactions: StateFlow<List<ReactionDisplay>> = _reactions.asStateFlow()

    private val _isLoadingReactions = MutableStateFlow(false)
    val isLoadingReactions: StateFlow<Boolean> = _isLoadingReactions.asStateFlow()

    private val _selectedReactionTab = MutableStateFlow<ReactionTypeEnum?>(null)
    val selectedReactionTab: StateFlow<ReactionTypeEnum?> = _selectedReactionTab.asStateFlow()

    fun openReactionList(contentType: String, objectId: Int) {
        _reactionListState.value = ReactionListState(contentType, objectId)
        _selectedReactionTab.value = null
        loadReactions(contentType, objectId, null)
    }

    fun dismissReactionList() {
        _reactionListState.value = null
        _reactions.value = emptyList<ReactionDisplay>()
    }

    fun setReactionTab(tab: ReactionTypeEnum?) {
        _selectedReactionTab.value = tab
        val state = _reactionListState.value ?: return
        loadReactions(state.contentType, state.objectId, tab)
    }

    private fun loadReactions(contentType: String, objectId: Int, reactionType: ReactionTypeEnum?) {
        viewModelScope.launch {
            _isLoadingReactions.value = true
            // Using getObjectLikes for now as it provides paginated reactions
            // We might need to filter by reactionType if the API supports it
            reactionRepository.getObjectLikes(contentType, objectId).fold(
                onSuccess = { response ->
                    val results = response.data?.pagination?.results ?: emptyList()
                    _reactions.value = if (reactionType == null) {
                        results
                    } else {
                        results.filter { it.reactionType == reactionType }
                    }
                    _isLoadingReactions.value = false
                },
                onFailure = {
                    _isLoadingReactions.value = false
                }
            )
        }
    }

    fun sendReaction(request: ReactionCreateRequest) {
        viewModelScope.launch {
            reactionRepository.createReaction(request).fold(
                onSuccess = { response ->
                    _reactionEvents.emit(
                        ReactionResult.Success(
                            contentType = request.contentType,
                            objectId = request.objectId,
                            reacted = response.data.reacted,
                            reactionType = response.data.reactionType,
                            reactionCount = response.data.reactionCount,
                            counts = response.data.counts
                        )
                    )
                },
                onFailure = { error ->
                    _reactionEvents.emit(
                        ReactionResult.Error(request.objectId, error.message ?: "Unknown error")
                    )
                }
            )
        }
    }
}



