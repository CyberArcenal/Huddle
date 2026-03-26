package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.ViewCreateRequest
import com.cyberarcenal.huddle.api.models.ViewDisplay
import com.cyberarcenal.huddle.data.repositories.ViewsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ViewManager(
    private val viewsRepository: ViewsRepository,
    private val viewModelScope: CoroutineScope
) {
    private val _viewEvents = MutableSharedFlow<ViewResult>()
    val viewEvents: SharedFlow<ViewResult> = _viewEvents.asSharedFlow()

    fun recordView(targetType: String, targetId: Int, durationSeconds: Int = 0) {
        viewModelScope.launch {
            val request = ViewCreateRequest(
                targetType = targetType,
                targetId = targetId,
                durationSeconds = durationSeconds
            )
            viewsRepository.recordView(request).fold(
                onSuccess = { viewDisplay ->
                    _viewEvents.emit(ViewResult.Success(viewDisplay))
                },
                onFailure = { error ->
                    _viewEvents.emit(ViewResult.Error(targetId, error.message ?: "Unknown error"))
                }
            )
        }
    }
}

sealed class ViewResult {
    data class Success(val viewDisplay: ViewDisplay) : ViewResult()
    data class Error(val targetId: Int, val message: String) : ViewResult()
}