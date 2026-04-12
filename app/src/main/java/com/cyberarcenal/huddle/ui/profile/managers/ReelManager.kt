package com.cyberarcenal.huddle.ui.profile.managers

import android.content.Context
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.data.repositories.ReelsRepository
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReelManager(
    private var userId: Int?,
    private val reelsRepository: ReelsRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private var reelObservationJob: Job? = null

    private val _userReels = MutableStateFlow<List<ReelDisplay>>(emptyList())
    val userReels: StateFlow<List<ReelDisplay>> = _userReels.asStateFlow()

    init {
        observeReels()
    }

    fun updateUserId(newUserId: Int?) {
        if (userId != newUserId) {
            userId = newUserId
            observeReels()
        }
    }

    private fun observeReels() {
        reelObservationJob?.cancel()
        userId?.let { uid ->
            reelObservationJob = viewModelScope.launch {
                reelsRepository.observeReels(uid).collectLatest { reels ->
                    _userReels.value = reels
                }
            }
        }
    }

    fun loadUserReels(context: Context) {
        viewModelScope.launch {
            reelsRepository.fetchAndCacheReels(userId, context).onFailure { error ->
                actionState.value = ActionState.Error(error.message ?: "Failed to load reels")
            }
        }
    }

    fun loadPublicReels(targetUserId: Int?, context: Context) {
        if (targetUserId == null) return
        viewModelScope.launch {
            reelsRepository.fetchAndCacheReels(targetUserId, context).onFailure { error ->
                actionState.value = ActionState.Error(error.message ?: "Failed to load user reels")
            }
        }
    }
}
