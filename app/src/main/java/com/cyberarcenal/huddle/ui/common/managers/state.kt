package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionCount
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum

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
data class PostDetailSheetState(val post: PostFeed)

sealed class ActionState {
    object Idle : ActionState()
    data class Loading(val message: String? = null) : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}