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
import androidx.paging.cachedIn
import com.cyberarcenal.huddle.api.models.Comment
import com.cyberarcenal.huddle.api.models.CommentCreateRequest
import com.cyberarcenal.huddle.api.models.LikeContentTypeEnum
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.UserProfile
import com.cyberarcenal.huddle.data.repositories.feed.FeedRepository
import com.cyberarcenal.huddle.data.repositories.users.ProfileRepository
import com.cyberarcenal.huddle.ui.feed.ActionState
import com.cyberarcenal.huddle.ui.feed.CommentSheetState
import com.cyberarcenal.huddle.ui.feed.OptionsSheetState
import com.cyberarcenal.huddle.utils.FileUtils
import com.cyberarcenal.huddle.utils.ImageValidator
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.filter
import kotlin.collections.map

class ProfileViewModel(
    application: Application,
    private val userId: Int?,
    private val repository: ProfileRepository,
    private val feedRepository: FeedRepository
) : AndroidViewModel(application) {

    // ========== STATE ==========
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _fullscreenImage = MutableStateFlow<String?>(null)
    val fullscreenImage: StateFlow<String?> = _fullscreenImage.asStateFlow()

    // For cropping flow
    enum class UploadType { PROFILE, COVER }  // 👈 ginawang public
    private val _uploadType = MutableStateFlow<UploadType?>(null)
    val uploadType: StateFlow<UploadType?> = _uploadType.asStateFlow()  // 👈 public access

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()
    private var _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: Int?) {
        _currentUserId.value = userId
    }

    init {
        loadProfile()
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
                ProfilePagingSource(idToLoad, repository)
            }.flow
        } else {
            flowOf(PagingData.empty())
        }
    }.cachedIn(viewModelScope)

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = if (userId == null) {
                repository.getCurrentUserProfile()
            } else {
                repository.getUserProfile(userId)
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
        val profile = (_profileState.value as? ProfileState.Success)?.profile ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading()
            val result = if (profile.isFollowing == true) {
                repository.unfollowUser(profile.id)
            } else {
                repository.followUser(profile.id)
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

    fun toggleLike(postId: Int?) {
        if (postId === null)return;
        viewModelScope.launch {
            feedRepository.toggleLike(LikeContentTypeEnum.POST, postId).fold(
                onSuccess = { /* optional */ },
                onFailure = { /* ignore or log */ }
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
            // Valid – itago ang URI at uri para sa crop
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
                maxHeight = 2304 // 16:9 approx
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
            setCompressionQuality(80)            // 75-85 recommended
            setCircleDimmedLayer(circleCrop)    // true for circular overlay
            setShowCropFrame(!circleCrop)
            setShowCropGrid(false)
            setHideBottomControls(false)
            setToolbarTitle("Crop profile photo")
        }

        // request square aspect and force result size (use 2x for retina if needed)
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
            setCompressionQuality(80) // 75-85 recommended
            setShowCropFrame(true)
            setShowCropGrid(true)
        }
        return UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(4f, 1f)        // 800x200 => 4:1
            .withMaxResultSize(800, 200)   // force output size
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
                UploadType.PROFILE -> repository.uploadProfilePicture(imagePart)
                UploadType.COVER -> repository.uploadCoverPhoto(imagePart)
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
            repository.removeProfilePicture().fold(
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
            repository.removeCoverPhoto().fold(
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

    // ========== BOTTOM SHEET STATES ==========
    private val _commentSheetState = MutableStateFlow<CommentSheetState?>(null)
    val commentSheetState: StateFlow<CommentSheetState?> = _commentSheetState.asStateFlow()

    private val _optionsSheetState = MutableStateFlow<OptionsSheetState?>(null)
    val optionsSheetState: StateFlow<OptionsSheetState?> = _optionsSheetState.asStateFlow()

    // Top-level comments (no parent)
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentsError = MutableStateFlow<String?>(null)
    val commentsError: StateFlow<String?> = _commentsError.asStateFlow()

    // Replies map: parentId -> list of replies
    private val _replies = MutableStateFlow<Map<Int, List<Comment>>>(emptyMap())
    val replies: StateFlow<Map<Int, List<Comment>>> = _replies.asStateFlow()

    private val _expandedReplies = MutableStateFlow<Set<Int>>(emptySet())
    val expandedReplies: StateFlow<Set<Int>> = _expandedReplies.asStateFlow()

    // Pagination for comments
    private var _commentPage = MutableStateFlow(1)
    private var _hasMoreComments = MutableStateFlow(true)
    private var _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPostId: Int? = null

    fun openCommentSheet(postId: Int?) {
        if (postId == null) return
        currentPostId = postId
        _commentSheetState.value = CommentSheetState(postId)
        // Reset pagination states
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

            feedRepository.getComments(postId = postId, page = page, pageSize = 20).fold(
                onSuccess = { paginated ->
                    val allComments = paginated.results
                    val topLevel = mutableListOf<Comment>()
                    val repliesMap = mutableMapOf<Int, MutableList<Comment>>()

                    allComments.forEach { comment ->
                        if (comment.parentComment == null) {
                            topLevel.add(comment)
                        } else {
                            repliesMap.getOrPut(comment.parentComment) { mutableListOf() }.add(comment)
                        }
                    }

                    if (replace) {
                        _comments.value = topLevel.reversed() // newest first
                        _replies.value = repliesMap
                    } else {
                        // Append new top-level comments and merge replies
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

    fun addComment(content: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Posting comment...")
            val request = CommentCreateRequest(
                postId = postId,
                content = content,
                parentCommentId = null
            )
            feedRepository.createComment(request).fold(
                onSuccess = { newComment ->
                    _comments.value = listOf(newComment) + _comments.value
                    _actionState.value =ActionState.Success("Comment added")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to post comment")
                }
            )
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            feedRepository.deleteComment(commentId).fold(
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
            feedRepository.deletePost(postId).fold(
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
            feedRepository.getReplies(commentId, page = 1, pageSize = 20).fold(
                onSuccess = { paginated ->
                    _replies.value = _replies.value + (commentId to paginated.results)
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to load replies")
                }
            )
        }
    }

    fun likeComment(commentId: Int?) {
        if (commentId == null) return
        viewModelScope.launch {
            feedRepository.toggleLike(LikeContentTypeEnum.COMMENT, commentId).fold(
                onSuccess = { response ->
                    // Update in top-level comments
                    _comments.value = _comments.value.map { comment ->
                        if (comment.id == commentId) {
                            comment.copy(
                                likeCount = response.likeCount ?: comment.likeCount,
                                hasLiked = response.liked ?: comment.hasLiked
                            )
                        } else comment
                    }
                    // Update in replies map
                    _replies.value = _replies.value.mapValues { (_, repliesList) ->
                        repliesList.map { reply ->
                            if (reply.id == commentId) {
                                reply.copy(
                                    likeCount = response.likeCount ?: reply.likeCount,
                                    hasLiked = response.liked ?: reply.hasLiked
                                )
                            } else reply
                        }
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Failed to like comment")
                }
            )
        }
    }

    fun addReply(parentCommentId: Int?, content: String) {
        if (parentCommentId == null) return
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("Posting reply...")
            val request = CommentCreateRequest(
                postId = postId,
                content = content,
                parentCommentId = parentCommentId
            )
            feedRepository.createComment(request).fold(
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

}

// ========== SEALED CLASSES ==========
sealed class LikeResult {
    data class Success(val postId: Int, val liked: Boolean, val likeCount: Int) : LikeResult()
    data class Error(val postId: Int, val message: String) : LikeResult()
}

data class CommentSheetState(val postId: Int)
data class OptionsSheetState(val post: PostFeed)

// ========== STATE CLASSES ==========
sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

//sealed class ActionState {
//    object Idle : ActionState()
//    data class Loading(val message: String? = null) : ActionState()
//    data class Success(val message: String) : ActionState()
//    data class Error(val message: String) : ActionState()
//}

// ========== FACTORY ==========
class ProfileViewModelFactory(
    private val userId: Int?,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                application = application,
                userId = userId,
                repository = ProfileRepository(),
                feedRepository = FeedRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}