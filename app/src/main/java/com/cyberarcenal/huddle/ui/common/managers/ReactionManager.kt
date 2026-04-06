package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionCount
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.repositories.ReactionsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ReactionManager(
    private val reactionRepository: ReactionsRepository,
    private val viewModelScope: CoroutineScope
) {
    private val _reactionEvents = MutableSharedFlow<ReactionResult>()
    val reactionEvents: SharedFlow<ReactionResult> = _reactionEvents.asSharedFlow()

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



