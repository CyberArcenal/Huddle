package com.cyberarcenal.huddle.ui.profile

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.CommentCreateRequest
import com.cyberarcenal.huddle.api.models.CommentDisplay
import com.cyberarcenal.huddle.api.models.FollowUserRequest
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.api.models.ReactionResponse
import com.cyberarcenal.huddle.api.models.UnfollowUserRequest
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.api.models.UserReactionA51Enum
import com.cyberarcenal.huddle.data.repositories.CommentsRepository
import com.cyberarcenal.huddle.data.repositories.FollowViewsRepository
import com.cyberarcenal.huddle.data.repositories.UserMediaRepository
import com.cyberarcenal.huddle.data.repositories.UserPostsRepository
import com.cyberarcenal.huddle.data.repositories.UserReactionsRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository
import com.cyberarcenal.huddle.ui.feed.ActionState
import com.cyberarcenal.huddle.ui.feed.CommentSheetState
import com.cyberarcenal.huddle.ui.feed.OptionsSheetState
import com.cyberarcenal.huddle.ui.feed.ReactionResult
import com.cyberarcenal.huddle.utils.FileUtils
import com.cyberarcenal.huddle.utils.ImageValidator
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.emptyList

class ProfileViewModel(
    application: Application,
    private val userId: Int?,
    private val userProfileRepository: UsersRepository,
    private val userFollowRepository: FollowViewsRepository,
    private val userMediaRepository: UserMediaRepository,
    private val postRepository: UserPostsRepository,
    private val commentRepository: CommentsRepository,
    private val reactionRepository: UserReactionsRepository,  // keep for posts
) : AndroidViewModel(application) {

    // ========== STATE ==========
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _fullscreenImage = MutableStateFlow<String?>(null)
    val fullscreenImage: StateFlow<String?> = _fullscreenImage.asStateFlow()

    // For cropping flow
    enum class UploadType { PROFILE, COVER }
    private val _uploadType = MutableStateFlow<UploadType?>(null)
    val uploadType: StateFlow<UploadType?> = _uploadType.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private var _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: Int?) {
        _currentUserId.value = userId
    }

    private val _reactionEvents = MutableSharedFlow<ReactionResult>()
    val reactionEvents = _reactionEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.CommentSuccess -> {
                        updateCommentReaction(
                            commentId = result.commentId,
                            liked = result.liked,
                            reactionType = result.reactionType as ReactionResponse.ReactionType,
                            likeCount = result.likeCount
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateCommentReaction(
        commentId: Int,
        liked: Boolean,
        reactionType: ReactionResponse.ReactionType?,
        likeCount: Int
    ) {
        // Update top-level comments
        _comments.update { currentComments ->
            currentComments.map { comment ->
                if (comment.id == commentId) {
                    comment.copy(
                        liked = liked,
                        userReaction = reactionType as UserReactionA51Enum,
                        likeCount = likeCount
                    )
                } else comment
            }
        }

        // Update replies
        _replies.update { currentReplies ->
            currentReplies.mapValues { (_, repliesList) ->
                repliesList.map { reply ->
                    if (reply.id == commentId) {
                        reply.copy(
                            liked = liked,
                            userReaction = reactionType as UserReactionA51Enum,
                            likeCount = likeCount
                        )
                    } else reply
                }
            }
        }
    }

    // Send reaction to comment
    fun sendCommentReaction(commentId: Int, reactionType: ReactionCreateRequest.ReactionType?) {
        viewModelScope.launch {
            val request = ReactionCreateRequest(
                contentType = "comment",
                objectId = commentId,
                reactionType = reactionType
            )
            reactionRepository.createReaction(request).fold(
                onSuccess = { response ->
                    _reactionEvents.emit(
                        ReactionResult.CommentSuccess(
                            commentId = commentId,
                            liked = response.reacted,
                            likeCount = response.counts.like ?: 0,
                            reactionType = response.reactionType as? ReactionCreateRequest.ReactionType
                        )
                    )
                },
                onFailure = { error ->
                    _reactionEvents.emit(
                        ReactionResult.Error(commentId, error.message ?: "Unknown error")
                    )
                }
            )
        }
    }



    // Update addComment to use new request format
    fun addComment(content: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Posting comment...")
            val request = CommentCreateRequest(
                targetType = "feed.post",
                targetId = postId,
                content = content,
                parentCommentId = null
            )
            commentRepository.createComment(request).fold(
                onSuccess = { newComment ->
                    _comments.value = listOf(newComment) + _comments.value
                    _actionState.value = ActionState.Success("Comment added")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to post comment")
                }
            )
        }
    }

    // Update addReply to use new request format
    fun addReply(parentCommentId: Int?, content: String) {
        if (parentCommentId == null) return
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Posting reply...")
            val request = CommentCreateRequest(
                targetType = "feed.post",
                targetId = postId,
                content = content,
                parentCommentId = parentCommentId
            )
            commentRepository.createComment(request).fold(
                onSuccess = { newReply ->
                    _replies.value = _replies.value.toMutableMap().apply {
                        val currentReplies = this[parentCommentId] ?: emptyList()
                        this[parentCommentId] = listOf(newReply) + currentReplies
                    }
                    _expandedReplies.value = _expandedReplies.value + parentCommentId
                    _actionState.value = ActionState.Success("Reply added")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to post reply")
                }
            )
        }
    }


    // ========== PROFILE DATA ==========
    @OptIn(ExperimentalCoroutinesApi::class)
    val userPostsFlow: Flow<PagingData<PostFeed>> = profileState.flatMapLatest { state ->
        val idToLoad = when {
            userId == null && state is ProfileState.Success -> state.profile.id
            userId != null -> userId
            else -> null
        }
        if (idToLoad != null) {
            Pager(PagingConfig(pageSize = 10)) {
                ProfilePagingSource(idToLoad, postRepository)
            }.flow
        } else {
            flowOf(PagingData.empty())
        }
    }.cachedIn(viewModelScope)

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = if (userId == null) {
                userProfileRepository.getProfile()
            } else {
                userProfileRepository.getPublicProfile(userId)
            }
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    _actionState.value = ActionState.Idle
                },
                onFailure = { error ->
                    _profileState.value = ProfileState.Error(error.message ?: "Failed to load profile")
                }
            )
        }
    }

    fun onFollowToggle() {
        val profile = (_profileState.value as? ProfileState.Success)?.profile ?: return;
        if(profile.id === null)return;
        viewModelScope.launch {
            _actionState.value = ActionState.Loading()
            val result = if (profile.isFollowing == true) {
                val request = UnfollowUserRequest(followingId = profile.id)
                userFollowRepository.unfollowUser(request)
            } else {
                val request = FollowUserRequest(followingId = profile.id)
                userFollowRepository.followUser(request)
            }
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success(if (profile.isFollowing == true) "Unfollowed" else "Followed")
                    loadProfile()
                },
                onFailure = {
                    _actionState.value = ActionState.Error(it.message ?: "Action failed")
                }
            )
        }
    }
    // ========== FULLSCREEN IMAGE ==========
    fun showFullscreenImage(url: String?) {
        _fullscreenImage.value = url
    }

    fun dismissFullscreenImage() {
        _fullscreenImage.value = null
    }

    // ========== IMAGE PICK & VALIDATION ==========
    fun onImagePickedForProfile(uri: Uri) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Validating image...")
            val validation = ImageValidator.validateImage(
                contentResolver = getApplication<Application>().contentResolver,
                uri = uri
            )
            if (!validation.isValid) {
                _validationError.value = validation.errorMessage
                _actionState.value = ActionState.Error(validation.errorMessage ?: "Invalid image")
                return@launch
            }
            _uploadType.value = UploadType.PROFILE
            _selectedImageUri.value = uri
            _actionState.value = ActionState.Idle
        }
    }

    fun onImagePickedForCover(uri: Uri) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Validating image...")
            val validation = ImageValidator.validateImage(
                contentResolver = getApplication<Application>().contentResolver,
                uri = uri,
                maxWidth = 4096,
                maxHeight = 2304
            )
            if (!validation.isValid) {
                _validationError.value = validation.errorMessage
                _actionState.value = ActionState.Error(validation.errorMessage ?: "Invalid image")
                return@launch
            }
            _uploadType.value = UploadType.COVER
            _selectedImageUri.value = uri
            _actionState.value = ActionState.Idle
        }
    }

    // ========== CROP INTENTS ==========
    fun startProfileCropIntent(context: Context, sourceUri: Uri, outputSize: Int = 400, circleCrop: Boolean = true): Intent {
        val destName = "cropped_profile_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(context.cacheDir, destName))

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(80)
            setCircleDimmedLayer(circleCrop)
            setShowCropFrame(!circleCrop)
            setShowCropGrid(false)
            setHideBottomControls(false)
            setToolbarTitle("Crop profile photo")
        }

        return UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(outputSize, outputSize)
            .getIntent(context)
    }

    fun startCoverCropIntent(context: Context, sourceUri: Uri): Intent {
        val destinationUri = Uri.fromFile(
            File(context.cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg")
        )
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(80)
            setShowCropFrame(true)
            setShowCropGrid(true)
        }
        return UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(4f, 1f)
            .withMaxResultSize(800, 200)
            .getIntent(context)
    }

    // ========== CROP RESULT HANDLING ==========
    fun onCropResult(croppedUri: Uri) {
        val type = _uploadType.value ?: run {
            _actionState.value = ActionState.Error("No pending upload")
            return
        }
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Uploading...")
            val result = FileUtils.uriToMultipartPart(getApplication(), croppedUri, "image_file")
                ?: run {
                    _actionState.value = ActionState.Error("Failed to process cropped image")
                    return@launch
                }
            val (imagePart, tempFile) = result

            val uploadResult = when (type) {
                UploadType.PROFILE -> userMediaRepository.uploadProfilePicture(imagePart)
                UploadType.COVER -> userMediaRepository.uploadCoverPhoto(imagePart)
            }

            uploadResult.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success(if (type == UploadType.PROFILE) "Profile picture updated" else "Cover photo updated")
                    loadProfile()
                    clearCropState()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Upload failed")
                }
            )
            tempFile.delete()
        }
    }

    private val _selectedPostImage = MutableStateFlow<String?>(null)
    val selectedPostImage: StateFlow<String?> = _selectedPostImage.asStateFlow()

    fun showPostImage(url: String) {
        _selectedPostImage.value = url
    }

    fun dismissPostImage() {
        _selectedPostImage.value = null
    }

    fun onCropError(errorMessage: String) {
        _actionState.value = ActionState.Error(errorMessage)
        clearCropState()
    }

    fun cancelCrop() {
        clearCropState()
    }

    private fun clearCropState() {
        _selectedImageUri.value = null
        _uploadType.value = null
        _validationError.value = null
    }

    // ========== REMOVE PHOTOS ==========
    fun removeProfilePicture() {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Removing profile picture...")
            userMediaRepository.removeProfilePicture().fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Profile picture removed")
                    loadProfile()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to remove")
                }
            )
        }
    }

    fun removeCoverPhoto() {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Removing cover photo...")
            userMediaRepository.removeCoverPhoto().fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Cover photo removed")
                    loadProfile()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to remove")
                }
            )
        }
    }

    // ========== BOTTOM SHEET STATES (unchanged logic, using new repositories) ==========
    private val _commentSheetState = MutableStateFlow<CommentSheetState?>(null)
    val commentSheetState: StateFlow<CommentSheetState?> = _commentSheetState.asStateFlow()

    private val _optionsSheetState = MutableStateFlow<OptionsSheetState?>(null)
    val optionsSheetState: StateFlow<OptionsSheetState?> = _optionsSheetState.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentDisplay>>(emptyList())
    val comments: StateFlow<List<CommentDisplay>> = _comments.asStateFlow()

    private val _commentsError = MutableStateFlow<String?>(null)
    val commentsError: StateFlow<String?> = _commentsError.asStateFlow()

    private val _replies = MutableStateFlow<Map<Int, List<CommentDisplay>>>(emptyMap())
    val replies: StateFlow<Map<Int, List<CommentDisplay>>> = _replies.asStateFlow()

    private val _expandedReplies = MutableStateFlow<Set<Int>>(emptySet())
    val expandedReplies: StateFlow<Set<Int>> = _expandedReplies.asStateFlow()

    private var _commentPage = MutableStateFlow(1)
    private var _hasMoreComments = MutableStateFlow(true)
    private var _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPostId: Int? = null

    fun openCommentSheet(postId: Int?) {
        if (postId == null) return
        currentPostId = postId
        _commentSheetState.value = CommentSheetState(postId)
        _commentPage.value = 1
        _hasMoreComments.value = true
        _comments.value = emptyList()
        _replies.value = emptyMap()
        loadComments(postId, page = 1, replace = true)
    }

    fun openOptionsSheet(post: PostFeed) {
        _optionsSheetState.value = OptionsSheetState(post)
    }

    fun dismissCommentSheet() {
        _commentSheetState.value = null
        _comments.value = emptyList()
        _commentsError.value = null
        _replies.value = emptyMap()
        _expandedReplies.value = emptySet()
        currentPostId = null
        _commentPage.value = 1
        _hasMoreComments.value = true
        _isLoadingMore.value = false
    }

    fun dismissOptionsSheet() {
        _optionsSheetState.value = null
    }

    private fun loadComments(postId: Int, page: Int, replace: Boolean) {
        viewModelScope.launch {
            if (page == 1) {
                _actionState.value = ActionState.Loading()
                _commentsError.value = null
            } else {
                _isLoadingMore.value = true
            }

            commentRepository.getComments(postId = postId, page = page, pageSize = 20).fold(
                onSuccess = { paginated ->
                    val allComments = paginated.results
                    val topLevel = mutableListOf<CommentDisplay>()
                    val repliesMap = mutableMapOf<Int, MutableList<CommentDisplay>>()

                    allComments.forEach { comment ->
                        if (comment.parentComment == null) {
                            topLevel.add(comment)
                        } else {
                            repliesMap.getOrPut(comment.parentComment) { mutableListOf() }.add(comment)
                        }
                    }

                    if (replace) {
                        _comments.value = topLevel.reversed()
                        _replies.value = repliesMap
                    } else {
                        _comments.value = (_comments.value + topLevel.reversed())
                        _replies.value = _replies.value.toMutableMap().apply {
                            repliesMap.forEach { (parentId, newReplies) ->
                                val existing = this[parentId] ?: emptyList()
                                this[parentId] = existing + newReplies
                            }
                        }
                    }

                    _hasMoreComments.value = paginated.hasNext
                    _commentPage.value = page + 1
                    _actionState.value = ActionState.Idle
                    _isLoadingMore.value = false
                },
                onFailure = { error ->
                    if (page == 1) {
                        _commentsError.value = error.message ?: "Failed to load comments"
                        _actionState.value = ActionState.Error(_commentsError.value!!)
                    } else {
                        _actionState.value = ActionState.Error(error.message ?: "Failed to load more comments")
                    }
                    _isLoadingMore.value = false
                }
            )
        }
    }

    fun loadMoreComments() {
        val postId = currentPostId ?: return
        if (!_hasMoreComments.value || _isLoadingMore.value) return
        loadComments(postId, page = _commentPage.value, replace = false)
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            commentRepository.deleteComment(commentId).fold(
                onSuccess = {
                    _comments.value = _comments.value.filter { it.id != commentId }
                    _replies.value = _replies.value.filterKeys { it != commentId }
                    _actionState.value = ActionState.Success("Comment deleted")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete comment")
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
                    dismissOptionsSheet()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to delete post")
                }
            )
        }
    }

    fun sendPostReaction(objectId: Int,  reactionType:
    ReactionType?, contentType: String = "feed.post") {
        viewModelScope.launch {
            val request = ReactionCreateRequest(
                contentType = contentType,
                objectId = objectId,
                reactionType = reactionType
            )
            val result = reactionRepository.createReaction(request)
            result.onSuccess { response ->
                _reactionEvents.emit(
                    ReactionResult.PostSuccess(
                        postId = objectId,
                        liked = response.reacted,
                        reactionType = response.reactionType as ReactionCreateRequest.ReactionType,
                        counts = response.counts
                    )
                )
            }.onFailure { error ->
                _reactionEvents.emit(
                    ReactionResult.Error(objectId, error.message ?: "Unknown error")
                )
            }
        }
    }

    fun reportPost(postId: Int, reason: String) {
        // TODO: Implement report API call when available
        _actionState.value = ActionState.Success("Reported (not implemented)")
        dismissOptionsSheet()
    }

    fun toggleReplyExpansion(commentId: Int?) {
        if (commentId == null) return
        val current = _expandedReplies.value
        _expandedReplies.value = if (commentId in current) {
            current.minus(commentId)
        } else {
            current.plus(commentId)
        }
    }

    fun loadReplies(commentId: Int?) {
        if (commentId == null) return
        if (_replies.value.containsKey(commentId)) return
        viewModelScope.launch {
            commentRepository.getReplies(commentId, page = 1, pageSize = 20).fold(
                onSuccess = { paginated ->
                    _replies.value = _replies.value + (commentId to paginated.results)
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to load replies")
                }
            )
        }
    }
}

// ========== SEALED CLASSES ==========
sealed class LikeResult {
    data class Success(val postId: Int, val liked: Boolean, val likeCount: Int) : LikeResult()
    data class Error(val postId: Int, val message: String) : LikeResult()
}

data class CommentSheetState(val postId: Int)
data class OptionsSheetState(val post: PostFeed)

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}


// ========== FACTORY ==========
class ProfileViewModelFactory(
    private val userId: Int?,
    private val application: Application,
    private val userProfileRepository: UsersRepository,
    private val userFollowRepository: FollowViewsRepository,
    private val userMediaRepository: UserMediaRepository,
    private val postRepository: UserPostsRepository,
    private val commentRepository: CommentsRepository,
    private val reactionRepository: UserReactionsRepository,
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                application = application,
                userId = userId,
                userProfileRepository = userProfileRepository,
                userFollowRepository = userFollowRepository,
                userMediaRepository = userMediaRepository,
                postRepository = postRepository,
                commentRepository = commentRepository,
                reactionRepository = reactionRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}