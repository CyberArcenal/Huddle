//// ProfileViewModel.kt
//package com.cyberarcenal.huddle.ui.profile.managers
//
//import android.app.Application
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.net.Uri
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import androidx.paging.Pager
//import androidx.paging.PagingConfig
//import androidx.paging.PagingData
//import androidx.paging.cachedIn
//import com.cyberarcenal.huddle.api.models.*
//import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
//import com.cyberarcenal.huddle.data.models.MediaDetailData
//import com.cyberarcenal.huddle.data.repositories.*
//import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
//import com.cyberarcenal.huddle.ui.feed.ActionState
//import com.cyberarcenal.huddle.ui.feed.CommentSheetState
//import com.cyberarcenal.huddle.ui.feed.OptionsSheetState
//import com.cyberarcenal.huddle.ui.feed.ReactionResult
//import com.cyberarcenal.huddle.ui.profile.UserContentPagingSource
//import com.cyberarcenal.huddle.ui.profile.components.UserLikedPagingSource
//import com.cyberarcenal.huddle.ui.profile.components.UserMediaPagingSource
//import com.cyberarcenal.huddle.utils.ImageValidator
//import com.yalantis.ucrop.UCrop
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import java.io.File
//import java.time.OffsetDateTime
//
//enum class UploadType { PROFILE, COVER }
//
//// ==============================
////  MAIN VIEW MODEL
//// ==============================
//class ProfileViewModel(
//    application: Application,
//    private val userId: Int?,
//    private val userProfileRepository: UsersRepository,
//    private val userFollowRepository: FollowRepository,
//    private val userMediaRepository: UserMediaRepository,
//    private val postRepository: UserPostsRepository,
//    private val commentRepository: CommentsRepository,
//    private val reactionRepository: UserReactionsRepository,
//    private val userContentRepository: UserContentRepository,
//    private val sharePostsRepository: SharePostsRepository,
//    private val storiesRepository: StoriesRepository
//) : AndroidViewModel(application) {
//
//    // ----- Core State -----
//    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
//    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()
//
//    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
//    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
//
//    private val _fullscreenImageData = MutableStateFlow<MediaDetailData?>(null)
//    val fullscreenImage: StateFlow<MediaDetailData?> = _fullscreenImageData.asStateFlow()
//
//    private val _currentUserId = MutableStateFlow<Int?>(null)
//    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()
//
//    fun setCurrentUserId(userId: Int?) { _currentUserId.value = userId }
//
//    // ----- Paging Flows -----
//    val mediaGridFlow: Flow<PagingData<UserMediaItem>> = Pager(PagingConfig(20)) {
//        UserMediaPagingSource(userId, userMediaRepository, userId == null)
//    }.flow.cachedIn(viewModelScope)
//
//    val likedItemsFlow: Flow<PagingData<UnifiedContentItem>> = Pager(PagingConfig(10)) {
//        UserLikedPagingSource(userId, userContentRepository, userId == null)
//    }.flow.cachedIn(viewModelScope)
//
//    val userContentFlow: Flow<PagingData<UnifiedContentItem>> = Pager(PagingConfig(10)) {
//        UserContentPagingSource(userId, userContentRepository, userId == null)
//    }.flow.cachedIn(viewModelScope)
//
//    // ----- Profile Data -----
//    fun loadProfile() {
//        viewModelScope.launch {
//            _profileState.value = ProfileState.Loading
//            val result = if (userId == null) userProfileRepository.getProfile()
//            else userProfileRepository.getPublicProfile(userId)
//
//            result.fold(
//                onSuccess = { profile ->
//                    _profileState.value = ProfileState.Success(profile)
//                    _actionState.value = ActionState.Idle
//                    if (userId == null) highlightManager.loadUserHighlights()
//                },
//                onFailure = { error ->
//                    _profileState.value = ProfileState.Error(error.message ?: "Failed to load profile")
//                }
//            )
//        }
//    }
//
//    fun onFollowToggle() {
//        val profile = (_profileState.value as? ProfileState.Success)?.profile ?: return
//        if (profile.id == null) return
//        viewModelScope.launch {
//            _actionState.value = ActionState.Loading()
//            val result = if (profile.isFollowing == true) {
//                userFollowRepository.unfollowUser(UnfollowUserRequest(profile.id))
//            } else {
//                userFollowRepository.followUser(FollowUserRequest(profile.id))
//            }
//            result.fold(
//                onSuccess = {
//                    _actionState.value = ActionState.Success(if (profile.isFollowing == true) "Unfollowed" else "Followed")
//                    loadProfile()
//                },
//                onFailure = { _actionState.value = ActionState.Error(it.message ?: "Action failed") }
//            )
//        }
//    }
//
//    fun showFullscreenImage(data: MediaDetailData) { _fullscreenImageData.value = data }
//    fun dismissFullscreenImage() { _fullscreenImageData.value = null }
//
//    fun sendReaction(request: ReactionCreateRequest) = reactionManager.sendReaction(request)
//    fun sharePost(shareData: ShareRequestData) = sharePostInternal(shareData)
//
//    // ----- Delegated Managers -----
//    val imageManager = ProfileImageManager()
//    val commentManager = CommentManager()
//    val highlightManager = HighlightManager()
//    val reactionManager = ReactionManager()
//
//    // -----------------------------------------------------------------
//    //  Helper inner classes (modular)
//    // -----------------------------------------------------------------
//
//    // 1. Profile & Cover Image Management
//    inner class ProfileImageManager {
//
//
//        private val _uploadType = MutableStateFlow<UploadType?>(null)
//        val uploadType: StateFlow<UploadType?> = _uploadType.asStateFlow()
//
//        private val _selectedImageUri = MutableStateFlow<Uri?>(null)
//        val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()
//
//        private val _validationError = MutableStateFlow<String?>(null)
//        val validationError: StateFlow<String?> = _validationError.asStateFlow()
//
//        fun onImagePickedForProfile(uri: Uri) {
//            viewModelScope.launch {
//                _actionState.value = ActionState.Loading("Validating image...")
//                val validation = ImageValidator.validateImage(
//                    getApplication<Application>().contentResolver, uri
//                )
//                if (!validation.isValid) {
//                    _validationError.value = validation.errorMessage
//                    _actionState.value = ActionState.Error(validation.errorMessage ?: "Invalid image")
//                    return@launch
//                }
//                _uploadType.value = UploadType.PROFILE
//                _selectedImageUri.value = uri
//                _actionState.value = ActionState.Idle
//            }
//        }
//
//        fun onImagePickedForCover(uri: Uri) {
//            viewModelScope.launch {
//                _actionState.value = ActionState.Loading("Validating image...")
//                val validation = ImageValidator.validateImage(
//                    getApplication<Application>().contentResolver, uri,
//                    maxWidth = 4096, maxHeight = 2304
//                )
//                if (!validation.isValid) {
//                    _validationError.value = validation.errorMessage
//                    _actionState.value = ActionState.Error(validation.errorMessage ?: "Invalid image")
//                    return@launch
//                }
//                _uploadType.value = UploadType.COVER
//                _selectedImageUri.value = uri
//                _actionState.value = ActionState.Idle
//            }
//        }
//
//        fun startProfileCropIntent(context: Context, sourceUri: Uri): Intent {
//            val destName = "cropped_profile_${System.currentTimeMillis()}.jpg"
//            val destUri = Uri.fromFile(File(context.cacheDir, destName))
//            val options = UCrop.Options().apply {
//                setCompressionFormat(Bitmap.CompressFormat.JPEG)
//                setCompressionQuality(80)
//                setCircleDimmedLayer(true)
//                setShowCropFrame(false)
//                setShowCropGrid(false)
//                setHideBottomControls(false)
//                setToolbarTitle("Crop profile photo")
//            }
//            return UCrop.of(sourceUri, destUri)
//                .withOptions(options)
//                .withAspectRatio(1f, 1f)
//                .withMaxResultSize(400, 400)
//                .getIntent(context)
//        }
//
//        fun startCoverCropIntent(context: Context, sourceUri: Uri): Intent {
//            val destUri = Uri.fromFile(File(context.cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg"))
//            val options = UCrop.Options().apply {
//                setCompressionFormat(Bitmap.CompressFormat.JPEG)
//                setCompressionQuality(80)
//                setShowCropFrame(true)
//                setShowCropGrid(true)
//            }
//            return UCrop.of(sourceUri, destUri)
//                .withOptions(options)
//                .withAspectRatio(4f, 1f)
//                .withMaxResultSize(800, 200)
//                .getIntent(context)
//        }
//
//        fun onCropResult(croppedUri: Uri) {
//            val type = _uploadType.value ?: run {
//                _actionState.value = ActionState.Error("No pending upload")
//                return
//            }
//            uploadCroppedImage(croppedUri, type)
//        }
//
//        fun onCropError(errorMessage: String) {
//            _actionState.value = ActionState.Error(errorMessage)
//            clearCropState()
//        }
//
//        fun cancelCrop() = clearCropState()
//
//        fun removeProfilePicture() {
//            viewModelScope.launch {
//                _actionState.value = ActionState.Loading("Removing profile picture...")
//                userMediaRepository.removeProfilePicture().fold(
//                    onSuccess = {
//                        _actionState.value = ActionState.Success("Profile picture removed")
//                        loadProfile()
//                    },
//                    onFailure = { error ->
//                        _actionState.value = ActionState.Error(error.message ?: "Failed to remove")
//                    }
//                )
//            }
//        }
//
//        fun removeCoverPhoto() {
//            viewModelScope.launch {
//                _actionState.value = ActionState.Loading("Removing cover photo...")
//                userMediaRepository.removeCoverPhoto().fold(
//                    onSuccess = {
//                        _actionState.value = ActionState.Success("Cover photo removed")
//                        loadProfile()
//                    },
//                    onFailure = { error ->
//                        _actionState.value = ActionState.Error(error.message ?: "Failed to remove")
//                    }
//                )
//            }
//        }
//
//        private fun uploadCroppedImage(croppedUri: Uri, type: UploadType) {
//            viewModelScope.launch {
//                _actionState.value = ActionState.Loading("Uploading...")
//                val croppedFile = getFileFromUri(croppedUri) ?: run {
//                    _actionState.value = ActionState.Error("Failed to get cropped image file")
//                    return@launch
//                }
//                val mimeType = getMimeType(croppedUri) ?: "image/jpeg"
//                val uploadResult = when (type) {
//                    UploadType.PROFILE -> userMediaRepository.uploadProfilePicture(croppedFile, mimeType)
//                    UploadType.COVER -> userMediaRepository.uploadCoverPhoto(croppedFile, mimeType)
//                }
//                uploadResult.fold(
//                    onSuccess = {
//                        _actionState.value = ActionState.Success(
//                            if (type == UploadType.PROFILE) "Profile picture updated" else "Cover photo updated"
//                        )
//                        loadProfile()
//                        clearCropState()
//                    },
//                    onFailure = { error -> _actionState.value = ActionState.Error(error.message ?: "Upload failed") }
//                )
//                croppedFile.delete()
//            }
//        }
//
//        private fun getFileFromUri(uri: Uri): File? {
//            val contentResolver = getApplication<Application>().contentResolver
//            return try {
//                val inputStream = contentResolver.openInputStream(uri) ?: return null
//                val tempFile = File.createTempFile("cropped_", ".jpg", getApplication<Application>().cacheDir)
//                tempFile.outputStream().use { inputStream.copyTo(it) }
//                tempFile
//            } catch (e: Exception) { null }
//        }
//
//        private fun getMimeType(uri: Uri) = getApplication<Application>().contentResolver.getType(uri)
//
//        private fun clearCropState() {
//            _selectedImageUri.value = null
//            _uploadType.value = null
//            _validationError.value = null
//        }
//    }
//
//    // 2. Comment Management
//    inner class CommentManager {
//        private val _commentSheetState = MutableStateFlow<CommentSheetState?>(null)
//        val commentSheetState: StateFlow<CommentSheetState?> = _commentSheetState.asStateFlow()
//
//        private val _optionsSheetState = MutableStateFlow<OptionsSheetState?>(null)
//        val optionsSheetState: StateFlow<OptionsSheetState?> = _optionsSheetState.asStateFlow()
//
//        private val _comments = MutableStateFlow<List<CommentDisplay>>(emptyList())
//        val comments: StateFlow<List<CommentDisplay>> = _comments.asStateFlow()
//
//        private val _commentsError = MutableStateFlow<String?>(null)
//        val commentsError: StateFlow<String?> = _commentsError.asStateFlow()
//
//        private val _replies = MutableStateFlow<Map<Int, List<CommentDisplay>>>(emptyMap())
//        val replies: StateFlow<Map<Int, List<CommentDisplay>>> = _replies.asStateFlow()
//
//        private val _expandedReplies = MutableStateFlow<Set<Int>>(emptySet())
//        val expandedReplies: StateFlow<Set<Int>> = _expandedReplies.asStateFlow()
//
//        private var _commentPage = MutableStateFlow(1)
//        private var _hasMoreComments = MutableStateFlow(true)
//        private var _isLoadingMore = MutableStateFlow(false)
//        val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
//
//        private var currentCommentTarget: Pair<String, Int>? = null
//
//        fun openCommentSheet(contentType: String, objectId: Int) {
//            currentCommentTarget = contentType to objectId
//            _commentSheetState.value = CommentSheetState(contentType, objectId)
//            _commentPage.value = 1
//            _hasMoreComments.value = true
//            _comments.value = emptyList()
//            _replies.value = emptyMap()
//            loadComments(contentType, objectId, page = 1, replace = true)
//        }
//
//        fun openOptionsSheet(post: PostFeed) { _optionsSheetState.value = OptionsSheetState(post) }
//        fun dismissCommentSheet() { _commentSheetState.value = null; resetComments() }
//        fun dismissOptionsSheet() { _optionsSheetState.value = null }
//
//        fun loadMoreComments() {
//            val (contentType, objectId) = currentCommentTarget ?: return
//            if (!_hasMoreComments.value || _isLoadingMore.value) return
//            loadComments(contentType, objectId, page = _commentPage.value, replace = false)
//        }
//
//        fun addComment(content: String) {
//            val (contentType, objectId) = currentCommentTarget ?: return
//            viewModelScope.launch {
//                _actionState.value = ActionState.Loading("Posting comment...")
//                val request = CommentCreateRequest(contentType, objectId, content, null)
//                commentRepository.createComment(request).fold(
//                    onSuccess = { newComment ->
//                        _comments.value = listOf(newComment) + _comments.value
//                        _actionState.value = ActionState.Success("Comment added")
//                    },
//                    onFailure = { error ->
//                        _actionState.value = ActionState.Error(error.message ?: "Failed to post comment")
//                    }
//                )
//            }
//        }
//
//        fun deleteComment(commentId: Int) {
//            viewModelScope.launch {
//                commentRepository.deleteComment(commentId).fold(
//                    onSuccess = {
//                        _comments.value = _comments.value.filter { it.id != commentId }
//                        _replies.value = _replies.value.filterKeys { it != commentId }
//                        _actionState.value = ActionState.Success("Comment deleted")
//                    },
//                    onFailure = { error ->
//                        _actionState.value = ActionState.Error(error.message ?: "Failed to delete comment")
//                    }
//                )
//            }
//        }
//
//        fun addReply(parentCommentId: Int?, content: String) {
//            if (parentCommentId == null) return
//            val (contentType, objectId) = currentCommentTarget ?: return
//            viewModelScope.launch {
//                _actionState.value = ActionState.Loading("Posting reply...")
//                val request = CommentCreateRequest(contentType, objectId, content, parentCommentId)
//                commentRepository.createComment(request).fold(
//                    onSuccess = { newReply ->
//                        _replies.value = _replies.value.toMutableMap().apply {
//                            val current = this[parentCommentId] ?: emptyList()
//                            this[parentCommentId] = listOf(newReply) + current
//                        }
//                        _expandedReplies.value = _expandedReplies.value + parentCommentId
//                        _actionState.value = ActionState.Success("Reply added")
//                    },
//                    onFailure = { error ->
//                        _actionState.value = ActionState.Error(error.message ?: "Failed to post reply")
//                    }
//                )
//            }
//        }
//
//        fun toggleReplyExpansion(commentId: Int?) {
//            if (commentId == null) return
//            _expandedReplies.value = if (commentId in _expandedReplies.value) {
//                _expandedReplies.value.minus(commentId)
//            } else {
//                _expandedReplies.value.plus(commentId)
//            }
//        }
//
//        fun loadReplies(commentId: Int?) {
//            if (commentId == null || _replies.value.containsKey(commentId)) return
//            viewModelScope.launch {
//                commentRepository.getReplies(commentId, page = 1, pageSize = 20).fold(
//                    onSuccess = { paginated ->
//                        _replies.value = _replies.value + (commentId to paginated.results)
//                    },
//                    onFailure = { error ->
//                        _actionState.value = ActionState.Error(error.message ?: "Failed to load replies")
//                    }
//                )
//            }
//        }
//
//        fun updateCommentReaction(
//            commentId: Int,
//            reacted: Boolean,
//            reactionType: ReactionType?,
//            reactionCount: Int,
//            counts: ReactionCount
//        ) {
//            _comments.update { comments ->
//                comments.map { c ->
//                    if (c.id == commentId) {
//                        val updatedStats = c.statistics?.copy(reactionCount = reactionCount, reactions = counts)
//                        c.copy(statistics = updatedStats)
//                    } else c
//                }
//            }
//            _replies.update { repliesMap ->
//                repliesMap.mapValues { (_, list) ->
//                    list.map { r ->
//                        if (r.id == commentId) {
//                            val updatedStats = r.statistics?.copy(reactionCount = reactionCount, reactions = counts)
//                            r.copy(statistics = updatedStats)
//                        } else r
//                    }
//                }
//            }
//        }
//
//        private fun loadComments(contentType: String, objectId: Int, page: Int, replace: Boolean) {
//            viewModelScope.launch {
//                if (page == 1) {
//                    _actionState.value = ActionState.Loading()
//                    _commentsError.value = null
//                } else _isLoadingMore.value = true
//
//                commentRepository.getCommentsForObject(contentType, objectId, page=page, pageSize = 20).fold(
//                    onSuccess = { paginated ->
//                        val allComments = paginated.results
//                        val topLevel = mutableListOf<CommentDisplay>()
//                        val repliesMap = mutableMapOf<Int, MutableList<CommentDisplay>>()
//                        allComments.forEach { comment ->
//                            if (comment.parentComment == null) topLevel.add(comment)
//                            else repliesMap.getOrPut(comment.parentComment) { mutableListOf() }.add(comment)
//                        }
//
//                        if (replace) {
//                            _comments.value = topLevel.reversed()
//                            _replies.value = repliesMap
//                        } else {
//                            _comments.value = (_comments.value + topLevel.reversed())
//                            _replies.value = _replies.value.toMutableMap().apply {
//                                repliesMap.forEach { (parentId, newReplies) ->
//                                    val existing = this[parentId] ?: emptyList()
//                                    this[parentId] = existing + newReplies
//                                }
//                            }
//                        }
//
//                        _hasMoreComments.value = paginated.hasNext
//                        _commentPage.value = page + 1
//                        _actionState.value = ActionState.Idle
//                        _isLoadingMore.value = false
//                    },
//                    onFailure = { error ->
//                        if (page == 1) {
//                            _commentsError.value = error.message ?: "Failed to load comments"
//                            _actionState.value = ActionState.Error(_commentsError.value!!)
//                        } else _actionState.value = ActionState.Error(error.message ?: "Failed to load more comments")
//                        _isLoadingMore.value = false
//                    }
//                )
//            }
//        }
//
//        private fun resetComments() {
//            _comments.value = emptyList()
//            _commentsError.value = null
//            _replies.value = emptyMap()
//            _expandedReplies.value = emptySet()
//            currentCommentTarget = null
//            _commentPage.value = 1
//            _hasMoreComments.value = true
//            _isLoadingMore.value = false
//        }
//    }
//
//    // 3. Story Highlights Management
//    inner class HighlightManager {
//        private val _userHighlights = MutableStateFlow<List<StoryHighlight>>(emptyList())
//        val userHighlights: StateFlow<List<StoryHighlight>> = _userHighlights.asStateFlow()
//
//        private val _recentStories = MutableStateFlow<List<Story>>(emptyList())
//        val recentStories: StateFlow<List<Story>> = _recentStories.asStateFlow()
//
//        private val _isCreatingHighlight = MutableStateFlow(false)
//        val isCreatingHighlight: StateFlow<Boolean> = _isCreatingHighlight.asStateFlow()
//
//        fun loadUserHighlights() {
//            viewModelScope.launch {
//                storiesRepository.getHighlights().fold(
//                    onSuccess = { highlights -> _userHighlights.value = highlights },
//                    onFailure = { /* ignore */ }
//                )
//            }
//        }
//
//        fun loadRecentStories() {
//            viewModelScope.launch {
//                storiesRepository.getMyStories(includeExpired = true, page = 1, pageSize = 50).fold(
//                    onSuccess = { paginated ->
//                        val thirtyDaysAgo = OffsetDateTime.now().minusDays(30)
//                        val recent = paginated.results.filter { story ->
//                            story.createdAt?.let { it >= thirtyDaysAgo } ?: false
//                        }
//                        _recentStories.value = recent
//                    },
//                    onFailure = { _recentStories.value = emptyList() }
//                )
//            }
//        }
//
//        fun createHighlight(title: String, selectedStoryIds: List<Int>) {
//            viewModelScope.launch {
//                _isCreatingHighlight.value = true
//                val request = StoryHighlightCreateRequest(title = title, storyIds = selectedStoryIds)
//                storiesRepository.createHighlight(request).fold(
//                    onSuccess = {
//                        loadUserHighlights()
//                        _isCreatingHighlight.value = false
//                    },
//                    onFailure = { error ->
//                        _isCreatingHighlight.value = false
//                        _actionState.value = ActionState.Error(error.message ?: "Failed to add highlights")
//                    }
//                )
//            }
//        }
//    }
//
//    // 4. Reaction Management
//    inner class ReactionManager {
//        private val _reactionEvents = MutableSharedFlow<ReactionResult>()
//        val reactionEvents = _reactionEvents.asSharedFlow()
//
//        fun sendReaction(request: ReactionCreateRequest) {
//            viewModelScope.launch {
//                reactionRepository.createReaction(request).fold(
//                    onSuccess = { response ->
//                        _reactionEvents.emit(
//                            ReactionResult.Success(
//                                contentType = request.contentType,
//                                objectId = request.objectId,
//                                reacted = response.reacted,
//                                reactionType = response.reactionType as? ReactionType,
//                                reactionCount = response.reactionCount,
//                                counts = response.counts
//                            )
//                        )
//                    },
//                    onFailure = { error ->
//                        _reactionEvents.emit(
//                            ReactionResult.Error(request.objectId, error.message ?: "Unknown error")
//                        )
//                    }
//                )
//            }
//        }
//    }
//
//    // 5. Share Post
//    private fun sharePostInternal(shareData: ShareRequestData) {
//        viewModelScope.launch {
//            _actionState.value = ActionState.Loading("Sharing...")
//            val request = ShareCreateRequest(
//                contentType = shareData.contentType,
//                objectId = shareData.contentId,
//                caption = shareData.caption,
//                privacy = shareData.privacy,
//                group = shareData.groupId
//            )
//            sharePostsRepository.createShare(request).fold(
//                onSuccess = { _actionState.value = ActionState.Success("Shared successfully") },
//                onFailure = { error -> _actionState.value = ActionState.Error(error.message ?: "Failed to share") }
//            )
//        }
//    }
//
//    // 6. Delete Post
//    fun deletePost(postId: Int) {
//        viewModelScope.launch {
//            _actionState.value = ActionState.Loading("Deleting post...")
//            postRepository.deletePost(postId).fold(
//                onSuccess = {
//                    _actionState.value = ActionState.Success("Post deleted")
//                    commentManager.dismissOptionsSheet()
//                },
//                onFailure = { error -> _actionState.value = ActionState.Error(error.message ?: "Failed to delete post") }
//            )
//        }
//    }
//
//    fun reportPost(postId: Int, reason: String) {
//        _actionState.value = ActionState.Success("Reported (not implemented)")
//        commentManager.dismissOptionsSheet()
//    }
//}
//
//// -----------------------------------------------------------------
////  Data classes and sealed classes remain unchanged
//// -----------------------------------------------------------------
//data class CommentSheetState(val contentType: String, val objectId: Int)
//data class OptionsSheetState(val post: PostFeed)
//
//sealed class ProfileState {
//    object Loading : ProfileState()
//    data class Success(val profile: UserProfile) : ProfileState()
//    data class Error(val message: String) : ProfileState()
//}
//
//// -----------------------------------------------------------------
////  ViewModel Factory
//// -----------------------------------------------------------------
//class ProfileViewModelFactory(
//    private val userId: Int?,
//    private val application: Application,
//    private val userProfileRepository: UsersRepository,
//    private val userFollowRepository: FollowRepository,
//    private val userMediaRepository: UserMediaRepository,
//    private val postRepository: UserPostsRepository,
//    private val commentRepository: CommentsRepository,
//    private val reactionRepository: UserReactionsRepository,
//    private val userContentRepository: UserContentRepository,
//    private val sharePostsRepository: SharePostsRepository,
//    private val storiesRepository: StoriesRepository,
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return ProfileViewModel(
//                application = application,
//                userId = userId,
//                userProfileRepository = userProfileRepository,
//                userFollowRepository = userFollowRepository,
//                userMediaRepository = userMediaRepository,
//                postRepository = postRepository,
//                commentRepository = commentRepository,
//                reactionRepository = reactionRepository,
//                userContentRepository = userContentRepository,
//                sharePostsRepository = sharePostsRepository,
//                storiesRepository = storiesRepository,
//            ) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}