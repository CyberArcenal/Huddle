package com.cyberarcenal.huddle.ui.profile.managers

import android.app.Application
import android.content.Context
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
import com.cyberarcenal.huddle.ui.profile.UserReelsPagingSource
import com.cyberarcenal.huddle.ui.profile.components.UserLikedPagingSource
import com.cyberarcenal.huddle.ui.profile.components.UserMediaPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val reelsRepository: ReelsRepository,
    private val storiesRepository: StoriesRepository,
    private val followRepository: FollowRepository,
    private val groupRepository: GroupRepository,
    private val context: Context,
) : AndroidViewModel(application) {


    // Core state
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _recentMoots = MutableStateFlow<List<UserMinimal>>(emptyList())
    val recentMoots: StateFlow<List<UserMinimal>> = _recentMoots.asStateFlow()



    private val _fullscreenImageData = MutableStateFlow<MediaDetailData?>(null)
    val fullscreenImage: StateFlow<MediaDetailData?> = _fullscreenImageData.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    // Resolve the target user ID: use passed userId if available, otherwise currentUserId
    private val targetUserIdFlow = _currentUserId.map { currentId ->
        userId ?: currentId
    }.filterNotNull().distinctUntilChanged()

    val isOwnProfile: StateFlow<Boolean> = _currentUserId
        .map { currentId ->
            userId == null || (currentId != null && userId == currentId)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, userId == null)

    fun setCurrentUserId(id: Int?) {
        _currentUserId.value = id
        highlightManager.updateUserId(userId ?: id)
        reelManager.updateUserId(userId ?: id)
        // Trigger profile loading once current user ID is known
        loadProfile()
    }

    fun setSelectedFilter(filter: String?) {
        _selectedFilter.value = filter
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
        userId = userId,
        storiesRepository = storiesRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val reelManager = ReelManager(
        userId = userId,
        reelsRepository = reelsRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val reactionManager = ReactionManager(
        reactionRepository = reactionRepository,
        viewModelScope = viewModelScope
    )

    // Paging flows - Reactive to isOwnProfile changes to ensure correct source is used
    val photosFlow: Flow<PagingData<UserMediaItem>> = isOwnProfile.flatMapLatest { isOwn ->
        Pager(PagingConfig(20)) {
            UserMediaPagingSource(userId, userMediaRepository, isOwn, contentType = "image")
        }.flow
    }.cachedIn(viewModelScope)

    val videosFlow: Flow<PagingData<UserMediaItem>> = isOwnProfile.flatMapLatest { isOwn ->
        Pager(PagingConfig(20)) {
            UserMediaPagingSource(userId, userMediaRepository, isOwn, contentType = "video")
        }.flow
    }.cachedIn(viewModelScope)

    val likedItemsFlow: Flow<PagingData<UnifiedContentItem>> = isOwnProfile.flatMapLatest { isOwn ->
        Pager(PagingConfig(10)) {
            UserLikedPagingSource(userId, userContentRepository, isOwn)
        }.flow
    }.cachedIn(viewModelScope)

    val postsFlow: Flow<PagingData<UnifiedContentItem>> = isOwnProfile.flatMapLatest { isOwn ->
        Pager(PagingConfig(10)) {
            UserContentPagingSource(userId, userContentRepository, isOwn, contentType = "post")
        }.flow
    }.cachedIn(viewModelScope)

    val userContentFlow: Flow<PagingData<UnifiedContentItem>> = combine(
        isOwnProfile,
        _selectedFilter
    ) { isOwn, filter -> isOwn to filter }
        .flatMapLatest { (isOwn, filter) ->
            Pager(PagingConfig(10)) {
                UserContentPagingSource(userId, userContentRepository, isOwn, contentType = filter)
            }.flow
        }.cachedIn(viewModelScope)

    val userReelsFlow: Flow<PagingData<ReelDisplay>> = Pager(PagingConfig(20)) {
        UserReelsPagingSource(userId, reelsRepository)
    }.flow.cachedIn(viewModelScope)

    val groupMembershipStatuses: StateFlow<Map<Int, Boolean>> =
        _groupMembershipStatuses.asStateFlow()
    val joiningGroupIds: StateFlow<Map<Int, Boolean>> = groupManager.joiningGroupIds



    private var profileObservationJob: Job? = null

    init {
        observeProfileFromDb()
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

    private fun observeProfileFromDb() {
        profileObservationJob?.cancel()
        profileObservationJob = viewModelScope.launch {
            targetUserIdFlow.collectLatest { targetId ->
                userProfileRepository.observeProfile(targetId)?.collect { user ->
                    if (user != null) {
                        _profileState.value = ProfileState.Success(user)
                    } else if (_profileState.value !is ProfileState.Success) {
                        // Only show loading if we don't have data yet
                        _profileState.value = ProfileState.Loading
                    }
                }
            }
        }
    }


    fun manualRefresh() {
        viewModelScope.launch {
            val targetUserId = userId ?: currentUserId.value ?: return@launch
            _isRefreshing.value = true

            // If we don't have cached data, show loading state
            if (_profileState.value !is ProfileState.Success) {
                _profileState.value = ProfileState.Loading
            }

            userProfileRepository.refreshProfile(targetUserId, context = context).fold(
                onSuccess = { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    
                    if (isOwnProfile.value) {
                        highlightManager.loadUserHighlights(context)
                        reelManager.loadUserReels(context)
                    } else {
                        highlightManager.loadPublicHighlights(userId, context)
                        reelManager.loadPublicReels(userId, context)
                    }
                },
                onFailure = { error ->
                    // Offline support: if we already have Success state, keep it and show a message
                    if (_profileState.value is ProfileState.Success) {
                        _actionState.value = ActionState.Error("No internet connection. Showing cached data.")
                    } else {
                        _profileState.value = ProfileState.Error(error.message ?: "Failed to refresh profile")
                    }
                }
            )
            _isRefreshing.value = false
        }
    }

    // Para sa compatibility, puwedeng i-alias ang loadProfile sa manualRefresh
    fun loadProfile() = manualRefresh()

    override fun onCleared() {
        profileObservationJob?.cancel()
        super.onCleared()
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

    fun loadRecentMoots() {
        viewModelScope.launch {
            try {
                followRepository.getMutualFriends().fold(
                    onSuccess = { response ->
                        if (response.status){
                            _recentMoots.value = response.data.results
                        }else{
                            _actionState.value = ActionState.Error(response.message)
                        }

                    },
                    onFailure = {
                        _recentMoots.value = emptyList()
                    }
                )
            }catch (e:Exception){
                _actionState.value = ActionState.Error(e.message ?: "Failed to load recent moots")
            }
        }
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
    private val reelsRepository: ReelsRepository,
    private val storiesRepository: StoriesRepository,
    private val groupRepository: GroupRepository,
    private val context: Context,
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
                reelsRepository = reelsRepository,
                storiesRepository = storiesRepository,
                followRepository = followRepository,
                groupRepository = groupRepository,
                context = context,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
