package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.data.repositories.ReelsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReelManager(
    private val reelsRepository: ReelsRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _localReelStates = MutableStateFlow<Map<Int, ReelState>>(emptyMap())
    val localReelStates: StateFlow<Map<Int, ReelState>> = _localReelStates.asStateFlow()

    data class ReelState(
        val hasLiked: Boolean,
        val likeCount: Int
    )

    fun updateLocalReelState(reelId: Int, isLiked: Boolean) {
        _localReelStates.update { current ->
            val currentState = current[reelId]
            val newCount = if (isLiked) {
                (currentState?.likeCount ?: 0) + 1
            } else {
                maxOf(0, (currentState?.likeCount ?: 1) - 1)
            }
            current + (reelId to ReelState(hasLiked = isLiked, likeCount = newCount))
        }
    }

    fun setReelStateFromServer(reelId: Int, isLiked: Boolean, likeCount: Int) {
        _localReelStates.update { current ->
            current + (reelId to ReelState(hasLiked = isLiked, likeCount = likeCount))
        }
    }
}