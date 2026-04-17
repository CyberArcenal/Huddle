package com.cyberarcenal.huddle.ui.common.managers

import com.cyberarcenal.huddle.api.models.CommentCreateRequest
import com.cyberarcenal.huddle.api.models.CommentDisplay
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCount
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.repositories.CommentsRepository
import com.cyberarcenal.huddle.ui.comments.CommentSheetState
import com.cyberarcenal.huddle.ui.profile.managers.OptionsSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class CommentManager(
    private val commentRepository: CommentsRepository,
    private val viewModelScope: CoroutineScope,
    private val actionState: MutableStateFlow<ActionState>
) {
    private val _commentSheetState = MutableStateFlow<CommentSheetState?>(null)
    val commentSheetState: StateFlow<CommentSheetState?> = _commentSheetState.asStateFlow()

    private val _initialCommentText = MutableStateFlow("")
    val initialCommentText: StateFlow<String> = _initialCommentText.asStateFlow()

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

    private var currentCommentTarget: Pair<String, Int>? = null

    fun openCommentSheet(contentType: String, objectId: Int, statistics: PostStatsSerializers? = null, initialText: String = "") {
        currentCommentTarget = contentType to objectId
        _initialCommentText.value = initialText
        _commentSheetState.value = CommentSheetState(contentType, objectId, statistics)
        _commentPage.value = 1
        _hasMoreComments.value = true
        _comments.value = emptyList()
        _replies.value = emptyMap()
        loadComments(contentType, objectId, page = 1, replace = true)
    }

    fun openOptionsSheet(post: PostFeed) { _optionsSheetState.value = OptionsSheetState(post)
    }
    fun dismissCommentSheet() { _commentSheetState.value = null; resetComments() }
    fun dismissOptionsSheet() { _optionsSheetState.value = null }

    fun loadMoreComments() {
        val (contentType, objectId) = currentCommentTarget ?: return
        if (!_hasMoreComments.value || _isLoadingMore.value) return
        loadComments(contentType, objectId, page = _commentPage.value, replace = false)
    }

    fun addComment(content: String) {
        val (contentType, objectId) = currentCommentTarget ?: return
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Posting comment...")
            val request = CommentCreateRequest(contentType, objectId, content, null)
            commentRepository.createComment(request).fold(
                onSuccess = { response ->
                    if (response.status){
                        val newComment = response.data?.comment
                        if (newComment !== null){
                            _comments.value = listOf(newComment) + _comments.value
                            actionState.value = ActionState.Success("Comment added")
                        }

                    }else{
                        actionState.value = ActionState.Error(response.message)
                    }

                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to post comment")
                }
            )
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            commentRepository.deleteComment(commentId).fold(
                onSuccess = {
                    _comments.value = _comments.value.filter { it.id != commentId }
                    _replies.value = _replies.value.filterKeys { it != commentId }
                    actionState.value = ActionState.Success("Comment deleted")
                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to delete comment")
                }
            )
        }
    }

    fun addReply(parentCommentId: Int?, content: String) {
        if (parentCommentId == null) return
        val (contentType, objectId) = currentCommentTarget ?: return
        viewModelScope.launch {
            actionState.value = ActionState.Loading("Posting reply...")
            val request = CommentCreateRequest(contentType, objectId, content, parentCommentId)
            commentRepository.createComment(request).fold(
                onSuccess = { response ->
                    if (response.status){
                        val newReply = response.data?.comment ?: return@fold
                        _replies.value = _replies.value.toMutableMap().apply {
                            val current = this[parentCommentId] ?: emptyList()
                            this[parentCommentId] = listOf(newReply) + current
                        }
                        _expandedReplies.value = _expandedReplies.value + parentCommentId
                        actionState.value = ActionState.Success("Reply added")
                    }else{
                        actionState.value = ActionState.Error(response.message)
                    }

                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to post reply")
                }
            )
        }
    }

    fun toggleReplyExpansion(commentId: Int?) {
        if (commentId == null) return
        _expandedReplies.value = if (commentId in _expandedReplies.value) {
            _expandedReplies.value.minus(commentId)
        } else {
            _expandedReplies.value.plus(commentId)
        }
    }

    fun loadReplies(commentId: Int?) {
        if (commentId == null || _replies.value.containsKey(commentId)) return
        viewModelScope.launch {
            commentRepository.getReplies(commentId, page = 1, pageSize = 20).fold(
                onSuccess = { paginated ->
                    if (paginated.status){
                        val results = paginated.data?.pagination?.results
                        if (results !== null){
                            _replies.value = _replies.value + (commentId to results)
                        }
                    }else{
                        actionState.value = ActionState.Error(paginated.message)
                    }

                },
                onFailure = { error ->
                    actionState.value = ActionState.Error(error.message ?: "Failed to load replies")
                }
            )
        }
    }

    fun updateCommentReaction(
        commentId: Int,
        reacted: Boolean,
        reactionType: ReactionTypeEnum?,
        reactionCount: Int,
        counts: ReactionCount
    ) {
        _comments.update { comments ->
            comments.map { c ->
                if (c.id == commentId) {
                    val currentStats = c.statistics
                    val updatedStats = if (currentStats != null) {
                        currentStats.copy(
                            liked = reacted,
                            reactionCount = reactionCount,
                            reactions = counts,
                            currentReaction = reactionType?.value ?: "",
                            // Provide defaults for non-nullable fields to avoid NPE
                            createdAt = currentStats.createdAt ?: java.time.OffsetDateTime.now()
                        )
                    } else {
                        // This case is unlikely for existing comments but good for safety
                        null
                    }
                    c.copy(statistics = updatedStats)
                } else c
            }
        }
        _replies.update { repliesMap ->
            repliesMap.mapValues { (_, list) ->
                list.map { r ->
                    if (r.id == commentId) {
                        val currentStats = r.statistics
                        val updatedStats = if (currentStats != null) {
                            currentStats.copy(
                                liked = reacted,
                                reactionCount = reactionCount,
                                reactions = counts,
                                currentReaction = reactionType?.value ?: "",
                                // Provide defaults for non-nullable fields to avoid NPE
                                createdAt = currentStats.createdAt ?: java.time.OffsetDateTime.now()
                            )
                        } else {
                            null
                        }
                        r.copy(statistics = updatedStats)
                    } else r
                }
            }
        }
    }

    private fun loadComments(contentType: String, objectId: Int, page: Int, replace: Boolean) {
        viewModelScope.launch {
            if (page == 1) {
                actionState.value = ActionState.Loading()
                _commentsError.value = null
            } else _isLoadingMore.value = true

            commentRepository.getCommentsForObject(contentType, objectId, page=page, pageSize = 20)
                .fold(
                onSuccess = { response ->
                    if (response.status){

                        val paginated = response.data?.pagination
                        if (paginated !== null){
                            val allComments = paginated.results
                            val topLevel = mutableListOf<CommentDisplay>()
                            val repliesMap = mutableMapOf<Int, MutableList<CommentDisplay>>()
                            allComments.forEach { comment ->
                                if (comment.parentComment == null) topLevel.add(comment)
                                else repliesMap.getOrPut(comment.parentComment) { mutableListOf() }.add(comment)
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
                            actionState.value = ActionState.Idle
                            _isLoadingMore.value = false
                        }

                    }

                },
                onFailure = { error ->
                    if (page == 1) {
                        _commentsError.value = error.message ?: "Failed to load comments"
                        actionState.value = ActionState.Error(_commentsError.value!!)
                    } else actionState.value = ActionState.Error(error.message ?: "Failed to load more comments")
                    _isLoadingMore.value = false
                }
            )
        }
    }

    private fun resetComments() {
        _comments.value = emptyList()
        _commentsError.value = null
        _replies.value = emptyMap()
        _expandedReplies.value = emptySet()
        currentCommentTarget = null
        _commentPage.value = 1
        _hasMoreComments.value = true
        _isLoadingMore.value = false
    }
}