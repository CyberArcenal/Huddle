package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.api.models.ShareCreateRequest
import com.cyberarcenal.huddle.data.repositories.SharePostsRepository
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch




class ShareManager(
    private val shareRepository: SharePostsRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _shareEvents = MutableSharedFlow<ShareResult>()
    val shareEvents: SharedFlow<ShareResult> = _shareEvents.asSharedFlow()

    fun sharePost(shareData: ShareRequestData) {
        viewModelScope.launch {
            val request = ShareCreateRequest(
                contentType = shareData.contentType,
                objectId = shareData.contentId,
                caption = shareData.caption,
                privacy = shareData.privacy,
                group = shareData.groupId
            )
            val result = shareRepository.createShare(request).fold(
                onSuccess = {
                    if (it.status){
                        _shareEvents.emit(ShareResult.Success(shareData.contentId, shareData
                            .contentType, it.message))
                    }else{
                        _shareEvents.emit(ShareResult.Error(shareData.contentId, it.message))
                    }
                },
                onFailure = { error ->
                    _shareEvents.emit(ShareResult.Error(shareData.contentId, error.message ?: "Unknown error"))
                }
            )
        }
    }
}

sealed class ShareResult {
    data class Success(val objectId: Int, val contentType: String, val message: String) : ShareResult()
    data class Error(val objectId: Int, val message: String) : ShareResult()
}