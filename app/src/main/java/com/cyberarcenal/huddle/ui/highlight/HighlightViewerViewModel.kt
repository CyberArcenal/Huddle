package com.cyberarcenal.huddle.ui.highlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.StoryHighlight
import com.cyberarcenal.huddle.api.models.UrgentReport
import com.cyberarcenal.huddle.data.repositories.CommentsRepository
import com.cyberarcenal.huddle.data.repositories.ReactionsRepository
import com.cyberarcenal.huddle.data.repositories.SharePostsRepository
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import com.cyberarcenal.huddle.ui.common.managers.CommentManager
import com.cyberarcenal.huddle.ui.common.managers.ReactionManager
import com.cyberarcenal.huddle.ui.common.managers.ShareManager
import com.cyberarcenal.huddle.ui.common.managers.ViewManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HighlightCarouselViewModel(
    private val highlights: List<StoryHighlight>,
    private val startIndex: Int,
    private val viewManager: ViewManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HighlightCarouselUiState>(HighlightCarouselUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent = _closeEvent.asSharedFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private var currentHighlightIndex = startIndex
    private var currentStoryIndex = 0

    init {
        loadCurrentStory()
    }

    val commentManager = CommentManager(CommentsRepository(), viewModelScope, _actionState)
    val shareManager = ShareManager(SharePostsRepository(), viewModelScope, _actionState)

    val reactionManager = ReactionManager(ReactionsRepository(), viewModelScope)

    private fun loadCurrentStory() {
        if (currentHighlightIndex !in highlights.indices) {
            _uiState.value = HighlightCarouselUiState.Error("No more highlights")
            return
        }
        val stories = highlights[currentHighlightIndex].stories ?: emptyList()
        if (stories.isEmpty()) {
            moveToNextHighlight()
            return
        }
        if (currentStoryIndex !in stories.indices) {
            currentStoryIndex = 0
        }
        _uiState.value = HighlightCarouselUiState.Success(
            currentStory = stories[currentStoryIndex],
            totalStoriesInCurrentHighlight = stories.size,
            currentStoryIndex = currentStoryIndex
        )
        val story = stories[currentStoryIndex]
        if (story.hasViewed == false && story.id != null) {
            viewManager.recordView("story", story.id, 5)
        }
    }

    fun nextStory() {
        val currentState = _uiState.value as? HighlightCarouselUiState.Success ?: return
        val stories = highlights[currentHighlightIndex].stories ?: emptyList()
        if (currentStoryIndex + 1 < stories.size) {
            currentStoryIndex++
            loadCurrentStory()
        } else {
            moveToNextHighlight()
        }
    }

    fun previousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            loadCurrentStory()
        } else if (currentHighlightIndex > 0) {
            currentHighlightIndex--
            val prevStories = highlights[currentHighlightIndex].stories ?: emptyList()
            currentStoryIndex = (prevStories.size - 1).coerceAtLeast(0)
            loadCurrentStory()
        }
    }

    private fun moveToNextHighlight() {
        if (currentHighlightIndex + 1 < highlights.size) {
            currentHighlightIndex++
            currentStoryIndex = 0
            loadCurrentStory()
        } else {
            viewModelScope.launch { _closeEvent.emit(Unit) }
        }
    }

    fun close() {
        viewModelScope.launch { _closeEvent.emit(Unit) }
    }

    fun sendReaction(data: ReactionCreateRequest) = reactionManager.sendReaction(data)

    // Comments – delegate to manager
    fun openCommentSheet(contentType: String, objectId: Int, stats: PostStatsSerializers?) =
        commentManager

    fun sharePost(shareData: ShareRequestData) = shareManager.sharePost(shareData)
    fun onMoreClick() {}
}

class HighlightCarouselViewModelFactory(
    private val highlights: List<StoryHighlight>,
    private val startIndex: Int,
    private val viewManager: ViewManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HighlightCarouselViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HighlightCarouselViewModel(highlights, startIndex, viewManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}