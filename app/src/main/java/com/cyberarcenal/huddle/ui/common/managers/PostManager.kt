package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import com.cyberarcenal.huddle.ui.profile.managers.OptionsSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostManager(
    private val postRepository: UserPostsRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _optionsSheetState = MutableStateFlow<OptionsSheetState?>(null)
    val optionsSheetState: StateFlow<OptionsSheetState?> = _optionsSheetState.asStateFlow()

    fun openOptionsSheet(post: PostFeed) {
        _optionsSheetState.value = OptionsSheetState(post)
    }

    fun dismissOptionsSheet() {
        _optionsSheetState.value = null
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Deleting post...")
            postRepository.deletePost(postId).fold(
                onSuccess = {
                    actionState.value = ActionState.Success("Post deleted")
                    dismissOptionsSheet()
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to delete post")
                }
            )
        }
    }

    fun reportPost(postId: Int, reason: String) {
        // Implement report logic here if repository supports it
        actionState.value = ActionState.Success("Reported (not implemented)")
        dismissOptionsSheet()
    }
}