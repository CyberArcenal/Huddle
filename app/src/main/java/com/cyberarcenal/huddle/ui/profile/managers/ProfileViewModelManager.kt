package com.cyberarcenal.huddle.ui.profile.managers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.CommentManager
import com.cyberarcenal.huddle.ui.common.managers.FollowManager
import com.cyberarcenal.huddle.ui.common.managers.GroupManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionResult
import com.cyberarcenal.huddle.ui.profile.UserContentPagingSource
import com.cyberarcenal.huddle.ui.profile.components.UserLikedPagingSource
import com.cyberarcenal.huddle.ui.profile.components.UserMediaPagingSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    private val userId: Int?,
    private val userProfileRepository: UsersRepository,
    private val userMediaRepository: UserMediaRepository,
    private val postRepository: UserPostsRepository,
    private val commentRepository: CommentsRepository,
    private val reactionRepository: ReactionsRepository,
    private val userContentRepository: UserContentRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val storiesRepository: StoriesRepository,
    private val followRepository: FollowRepository,
    private val groupRepository: GroupRepository,
) : AndroidViewModel(application) {

    // Core state
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _fullscreenImageData = MutableStateFlow<MediaDetailData?>(null)
    val fullscreenImage: StateFlow<MediaDetailData?> = _fullscreenImageData.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    fun setCurrentUserId(id: Int?) {
        _currentUserId.value = id
        // Pagkatapos ma-set ang currentUserId, i-load ang profile para tama ang logic sa loadProfile()
        loadProfile()
    }


    private val _followStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    private val _groupMembershipStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())


    val groupManager = GroupManager(
        groupRepository = groupRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )
    val imageManager = ProfileImageManager(
        application = getApplication(),
        userMediaRepository = userMediaRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState,
        onProfileUpdated = { loadProfile() }
    )
    val followManager = FollowManager(
        followRepository = followRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val followStatuses: StateFlow<Map<Int, Boolean>> = followManager.followStatuses


    val commentManager = CommentManager(
        commentRepository = commentRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val highlightManager = HighlightManager(
        storiesRepository = storiesRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val reactionManager = ReactionManager(
        reactionRepository = reactionRepository,
        viewModelScope = viewModelScope
    )

    // Paging flows
    val isOwnProfile: Boolean
        get() = userId == null || (currentUserId.value != null && userId == currentUserId.value)

    val mediaGridFlow: Flow<PagingData<UserMediaItem>> = Pager(PagingConfig(20)) {
        UserMediaPagingSource(userId, userMediaRepository, isOwnProfile)
    }.flow.cachedIn(viewModelScope)

    val likedItemsFlow: Flow<PagingData<UnifiedContentItem>> = Pager(PagingConfig(10)) {
        UserLikedPagingSource(userId, userContentRepository, isOwnProfile)
    }.flow.cachedIn(viewModelScope)

    val userContentFlow: Flow<PagingData<UnifiedContentItem>> = Pager(PagingConfig(10)) {
        UserContentPagingSource(userId, userContentRepository, isOwnProfile)
    }.flow.cachedIn(viewModelScope)

    val groupMembershipStatuses: StateFlow<Map<Int, Boolean>> =
        _groupMembershipStatuses.asStateFlow()
    val joiningGroupIds: StateFlow<Map<Int, Boolean>> = groupManager.joiningGroupIds

    init {
        // Alisin ang loadProfile sa init dahil tinatawag na ito sa setCurrentUserId
        // loadProfile()
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
                        when (result.contentType) {
                            "comment" -> {
                                commentManager.updateCommentReaction(
                                    commentId = result.objectId,
                                    reacted = result.reacted,
                                    reactionType = result.reactionType as ReactionTypeEnum?,
                                    reactionCount = result.reactionCount,
                                    counts = result.counts
                                )
                            }

                            "post", "reel" -> { /* update post/reel if needed */
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = if (isOwnProfile) {
                userProfileRepository.getProfile()
            } else {
                userProfileRepository.getPublicProfile(userId!!)
            }

            result.fold(
                onSuccess = { response ->
                    if (response.status) {
                        val profile = response.data.user
                        _profileState.value = ProfileState.Success(profile)
                        _actionState.value = ActionState.Idle

                        // I-set target user para sa follow button kung hindi sariling profile
                        if (!isOwnProfile && profile.id != null) {
                            followManager.setTargetUser(profile.id)
                        }

                        if (isOwnProfile) {
                            highlightManager.loadUserHighlights()
                        } else {
                            highlightManager.loadPublicHighlights(userId!!)
                        }
                    }
                },
                onFailure = { error ->
                    _profileState.value =
                        ProfileState.Error(error.message ?: "Failed to load profile")
                }
            )
        }
    }

    fun showFullscreenImage(data: MediaDetailData) {
        _fullscreenImageData.value = data
    }

    fun dismissFullscreenImage() {
        _fullscreenImageData.value = null
    }

    fun sharePost(shareData: ShareRequestData) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Sharing...")
            val request = ShareCreateRequest(
                contentType = shareData.contentType,
                objectId = shareData.contentId,
                caption = shareData.caption,
                privacy = shareData.privacy,
                group = shareData.groupId
            )
            sharePostsRepository.createShare(request).fold(
                onSuccess = { _actionState.value = ActionState.Success("Shared successfully") },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to share")
                }
            )
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Deleting post...")
            postRepository.deletePost(postId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Post deleted")
                    commentManager.dismissOptionsSheet()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete post")
                }
            )
        }
    }

    fun reportPost(postId: Int, reason: String) {
        _actionState.value = ActionState.Success("Reported (not implemented)")
        commentManager.dismissOptionsSheet()
    }
}

// Keep legacy for compatibility
data class CommentSheetState(val contentType: String, val objectId: Int)
data class OptionsSheetState(val post: PostFeed)

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModelFactory(
    private val userId: Int?,
    private val application: Application,
    private val userProfileRepository: UsersRepository,
    private val followRepository: FollowRepository,
    private val userMediaRepository: UserMediaRepository,
    private val postRepository: UserPostsRepository,
    private val commentRepository: CommentsRepository,
    private val reactionRepository: ReactionsRepository,
    private val userContentRepository: UserContentRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val storiesRepository: StoriesRepository,
    private val groupRepository: GroupRepository,
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                application = application,
                userId = userId,
                userProfileRepository = userProfileRepository,
                userMediaRepository = userMediaRepository,
                postRepository = postRepository,
                commentRepository = commentRepository,
                reactionRepository = reactionRepository,
                userContentRepository = userContentRepository,
                sharePostsRepository = sharePostsRepository,
                storiesRepository = storiesRepository,
                followRepository = followRepository,
                groupRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
