package com.cyberarcenal.huddle.ui.storyviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.*
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StoryFeedViewerViewModel(
    storyFeedsInput: List<StoryFeed>,
    private val startIndex: Int,
    private val viewManager: ViewManager,
    private val storyRepository: StoriesRepository,

    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
) : ViewModel() {
    // Make storyFeeds mutable so we can update it
    private val storyFeeds = storyFeedsInput.toMutableList()

    private val _uiState = MutableStateFlow<StoryFeedViewerUiState>(StoryFeedViewerUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent = _closeEvent.asSharedFlow()

    private val _storyOptionsSheetState = MutableStateFlow<Story?>(null)
    val storyOptionsSheetState: StateFlow<Story?> = _storyOptionsSheetState.asStateFlow()

    // Action state for messages
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private var currentUserIndex = startIndex
    private var currentStoryIndex = 0


    val commentManager = CommentManager(
        commentRepository = commentRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val followManager = FollowManager(
        followRepository = followRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    val reactionManager = ReactionManager(
        reactionRepository = reactionsRepository,
        viewModelScope = viewModelScope
    )

    val shareManager = ShareManager(
        shareRepository = sharePostsRepository,
        viewModelScope = viewModelScope,
        actionState = _actionState
    )

    // Expose comment manager state
    val commentSheetState = commentManager.commentSheetState
    val comments = commentManager.comments
    val commentsError = commentManager.commentsError
    val replies = commentManager.replies
    val expandedReplies = commentManager.expandedReplies
    val isLoadingMore = commentManager.isLoadingMore

    // Story options sheet


    init {
        loadCurrentStory()
        viewModelScope.launch {
            reactionManager.reactionEvents.collect { result ->
                when (result) {
                    is ReactionResult.Success -> {
                        updateStoryReaction(
                            storyId = result.objectId,
                            reacted = result.reacted,
                            reactionType = result.reactionType,
                            counts = result.counts
                        )
                    }
                    is ReactionResult.Error -> {
                        _actionState.value = ActionState.Error(result.message)
                    }
                }
            }
        }
    }

    private fun loadCurrentStory() {
        if (currentUserIndex !in storyFeeds.indices) {
            _uiState.value = StoryFeedViewerUiState.Error("No more stories")
            return
        }
        val userStories = storyFeeds[currentUserIndex].stories ?: emptyList()
        if (userStories.isEmpty()) {
            moveToNextUser()
            return
        }
        if (currentStoryIndex !in userStories.indices) {
            currentStoryIndex = 0
        }
        _uiState.value = StoryFeedViewerUiState.Success(
            currentStory = userStories[currentStoryIndex],
            totalStoriesInCurrentUser = userStories.size,
            currentStoryIndex = currentStoryIndex,
            currentUser = storyFeeds[currentUserIndex].user
        )
        val story = userStories[currentStoryIndex]
        if (story.hasViewed == false && story.id != null) {
            viewManager.recordView("story", story.id, 5)
        }
    }

    private fun updateStoryReaction(
        storyId: Int,
        reacted: Boolean,
        reactionType: ReactionTypeEnum?,
        counts: ReactionCount?
    ) {
        val state = _uiState.value as? StoryFeedViewerUiState.Success ?: return
        val currentStory = state.currentStory
        val currentStats = currentStory.statistics

        // Build updated statistics
        val newStats = if (currentStats != null) {
            currentStats.copy(
                liked = reacted,
                currentReaction = if (reacted) reactionType?.value else null,
                reactionCount = counts ?: currentStats.reactionCount
            )
        } else {
            // If no statistics exist, create a new one
            PostStatsSerializers(
                commentCount = 0,
                likeCount = if (reacted) 1 else 0,
                reactionCount = counts ?: ReactionCount(),
                privacy = PrivacyB23Enum.PUBLIC,
                comments = emptyList(),
                liked = reacted,
                shareCount = 0,
                viewCount = 0,
                mootsWhoReacted = emptyList(),
                uniqueViewers = 0,
                bookmarkCount = 0,
                reportCount = 0,
                isAuthor = false,
                createdAt = java.time.OffsetDateTime.now(),
                updatedAt = java.time.OffsetDateTime.now(),
                trendingScore = 0.0,
                currentReaction = if (reacted) reactionType?.value else null
            )
        }

        val updatedStory = currentStory.copy(statistics = newStats)

        // Update the story in the mutable list
        val userStories = storyFeeds[currentUserIndex].stories?.toMutableList()
        userStories?.let {
            val index = it.indexOfFirst { story -> story.id == storyId }
            if (index != -1) {
                it[index] = updatedStory
                // Now update the StoryFeed at the current user index with the new stories list
                storyFeeds[currentUserIndex] = storyFeeds[currentUserIndex].copy(stories = it)
            }
        }

        // Update the UI state with the updated story
        _uiState.value = state.copy(currentStory = updatedStory)
    }

    fun nextStory() {
        val currentState = _uiState.value as? StoryFeedViewerUiState.Success ?: return
        val userStories = storyFeeds[currentUserIndex].stories ?: emptyList()
        if (currentStoryIndex + 1 < userStories.size) {
            currentStoryIndex++
            loadCurrentStory()
        } else {
            moveToNextUser()
        }
    }

    fun previousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            loadCurrentStory()
        } else if (currentUserIndex > 0) {
            currentUserIndex--
            val prevUserStories = storyFeeds[currentUserIndex].stories ?: emptyList()
            currentStoryIndex = (prevUserStories.size - 1).coerceAtLeast(0)
            loadCurrentStory()
        }
    }

    private fun moveToNextUser() {
        if (currentUserIndex + 1 < storyFeeds.size) {
            currentUserIndex++
            currentStoryIndex = 0
            loadCurrentStory()
        } else {
            viewModelScope.launch { _closeEvent.emit(Unit) }
        }
    }

    fun close() {
        viewModelScope.launch { _closeEvent.emit(Unit) }
    }

    fun onReactionClick(reactionType: ReactionTypeEnum?) {
        val story = (_uiState.value as? StoryFeedViewerUiState.Success)?.currentStory ?: return
        val storyId = story.id ?: return
        reactionManager.sendReaction(
            ReactionCreateRequest(
                contentType = "story",
                objectId = storyId,
                reactionType = reactionType
            )
        )
    }

    fun onCommentClick() {
        val story = (_uiState.value as? StoryFeedViewerUiState.Success)?.currentStory ?: return
        val storyId = story.id ?: return
        commentManager.openCommentSheet("story", storyId)
    }

    fun onShareClick(shareData: ShareRequestData) {
        _actionState.value = ActionState.Success("Share feature coming soon")
        viewModelScope.launch {
            delay(2000)
            _actionState.value = ActionState.Idle
        }
    }

    fun onMoreClick() {
        val story = (_uiState.value as? StoryFeedViewerUiState.Success)?.currentStory ?: return
        _storyOptionsSheetState.value = story
    }

    fun dismissStoryOptionsSheet() {
        _storyOptionsSheetState.value = null
    }

    fun deleteStory(storyId: Int) {
        viewModelScope.launch {
            storyRepository.deleteStoryPermanent(storyId).onSuccess {
                _actionState.value = ActionState.Success("Story deleted")
                removeStoryFromFeed(storyId)
                dismissStoryOptionsSheet()
            }.onFailure {
                _actionState.value = ActionState.Error("Failed to delete story: ${it.message}")
            }
        }
    }

    fun archiveStory(storyId: Int) {
        viewModelScope.launch {
            storyRepository.deactivateStory(storyId).onSuccess {
                _actionState.value = ActionState.Success("Story archived")
                removeStoryFromFeed(storyId)
                dismissStoryOptionsSheet()
            }.onFailure {
                _actionState.value = ActionState.Error("Failed to archive story: ${it.message}")
            }
        }
    }

    fun addToHighlight(storyId: Int) {
        _actionState.value = ActionState.Success("Add to highlight feature coming soon")
        viewModelScope.launch {
            delay(2000)
            _actionState.value = ActionState.Idle
        }
        dismissStoryOptionsSheet()
    }

    fun saveStory(storyId: Int) {
        _actionState.value = ActionState.Success("Story saved to gallery")
        viewModelScope.launch {
            delay(2000)
            _actionState.value = ActionState.Idle
        }
        dismissStoryOptionsSheet()
    }

    private fun removeStoryFromFeed(storyId: Int) {
        val userStories = storyFeeds[currentUserIndex].stories?.toMutableList()
        userStories?.removeAll { it.id == storyId }
        if (userStories.isNullOrEmpty()) {
            // Remove the user's entire story feed if no stories left
            storyFeeds.removeAt(currentUserIndex)
            if (storyFeeds.isEmpty()) {
                close()
            } else {
                if (currentUserIndex >= storyFeeds.size) {
                    currentUserIndex = storyFeeds.size - 1
                }
                currentStoryIndex = 0
                loadCurrentStory()
            }
        } else {
            storyFeeds[currentUserIndex] = storyFeeds[currentUserIndex].copy(stories = userStories)
            if (currentStoryIndex >= userStories.size) {
                currentStoryIndex = (userStories.size - 1).coerceAtLeast(0)
            }
            loadCurrentStory()
        }
    }

    // Delegate comment manager methods
    fun dismissCommentSheet() = commentManager.dismissCommentSheet()
    fun loadMoreComments() = commentManager.loadMoreComments()
    fun addComment(content: String) = commentManager.addComment(content)
    fun deleteComment(commentId: Int) = commentManager.deleteComment(commentId)
    fun addReply(parentCommentId: Int?, content: String) = commentManager.addReply(parentCommentId, content)
    fun toggleReplyExpansion(commentId: Int?) = commentManager.toggleReplyExpansion(commentId)
    fun loadReplies(commentId: Int?) = commentManager.loadReplies(commentId)
}

class StoryFeedViewerViewModelFactory(
    private val storyFeeds: List<StoryFeed>,
    private val startIndex: Int,
    private val viewManager: ViewManager,
    private val storyRepository: StoriesRepository,

    private val commentRepository: CommentsRepository,
    private val reactionsRepository: ReactionsRepository,
    private val sharePostsRepository: SharePostsRepository,
    private val followRepository: FollowRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryFeedViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryFeedViewerViewModel(
                storyFeeds,
                startIndex,
                viewManager,
                storyRepository,
                commentRepository,
                reactionsRepository,
                sharePostsRepository,
                followRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}