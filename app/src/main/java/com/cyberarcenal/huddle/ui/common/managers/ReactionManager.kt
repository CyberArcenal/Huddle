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



// ========== SEALED CLASSES ==========
sealed class ReactionResult {
    data class Success(
        val contentType: String,
        val objectId: Int,
        val reacted: Boolean,
        val reactionType: ReactionTypeEnum?,
        val reactionCount: Int,
        val counts: ReactionCount
    ) : ReactionResult()
    data class Error(val id: Int, val message: String) : ReactionResult()
}

data class CommentSheetState(val contentType: String, val objectId: Int)
data class OptionsSheetState(val post: PostFeed)

sealed class ActionState {
    object Idle : ActionState()
    data class Loading(val message: String? = null) : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}