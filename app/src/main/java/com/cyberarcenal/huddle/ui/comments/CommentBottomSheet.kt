package com.cyberarcenal.huddle.ui.comments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.CommentDisplay
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.reactionPicker.ReactionPickerLayout
import com.cyberarcenal.huddle.ui.common.managers.ActionState

data class CommentSheetState(
    val contentType: String, val objectId: Int, val statistics: PostStatsSerializers? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    comments: List<CommentDisplay>,
    replies: Map<Int, List<CommentDisplay>>,
    expandedReplies: Set<Int>,
    currentUserId: Int?,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onToggleReplyExpanded: (Int?) -> Unit,
    onLoadReplies: (Int?) -> Unit,
    onReactToComment: (Int, ReactionTypeEnum?) -> Unit,
    onReplyToComment: (Int?, String) -> Unit,
    onReportComment: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit,
    onDeleteComment: (Int) -> Unit,
    actionState: ActionState,
    errorMessage: String?,
    statistics: PostStatsSerializers? = null,
    initialText: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Calculate 65% to 70% height
    val targetHeight = screenHeight * 0.68f

    var commentText by remember { mutableStateOf(initialText) }

    LaunchedEffect(initialText) {
        if (initialText.isNotEmpty()) {
            commentText = initialText
            focusRequester.requestFocus()
        }
    }
    var replyingToUser by remember { mutableStateOf<String?>(null) }
    var replyingToCommentId by remember { mutableStateOf<Int?>(null) }

    // Logic to handle pre-filled text (e.g., from mention)
    LaunchedEffect(statistics) {
        // If statistics changes or sheet re-opens, we might want to check for initial text
        // But for now, we just rely on external triggers or state management
    }
    
    // External trigger for mentions
    fun handleMention(username: String) {
        commentText = "@$username "
        focusRequester.requestFocus()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null && lastVisibleIndex >= comments.size - 3) {
                onLoadMore()
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
            }
        }) {
        ReactionPickerLayout(modifier = Modifier.fillMaxWidth().height(targetHeight)) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                StatisticsBar(
                    statistics = statistics,
                    loadedCommentCount = comments.size,
                    isLoadingMore = isLoadingMore,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Comments list
                Box(modifier = Modifier.weight(1f)) {
                    if (errorMessage != null && comments.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    } else if (comments.isEmpty() && !isLoadingMore) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No comments yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(comments, key = { it.id ?: it.hashCode() }) { comment ->
                                CommentItem(
                                    comment = comment,
                                    replies = replies[comment.id] ?: emptyList(),
                                    isExpanded = comment.id in expandedReplies,
                                    currentUserId = currentUserId,
                                    onToggleExpand = {
                                        onToggleReplyExpanded(comment.id)
                                        if (comment.id !in expandedReplies) {
                                            onLoadReplies(comment.id)
                                        }
                                    },
                                    onReact = onReactToComment,
                                    onReplyClick = { username ->
                                        replyingToUser = username
                                        replyingToCommentId = comment.id
                                        focusRequester.requestFocus()
                                    },
                                    onReport = { onReportComment(comment.id) },
                                    level = 0
                                )
                            }

                            if (isLoadingMore) {
                                item(key = "loading_more_indicator") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Section
                Surface(
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface).navigationBarsPadding()
                            .imePadding().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (replyingToUser != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Replying to @$replyingToUser",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(14.dp).clickable {
                                        replyingToUser = null
                                        replyingToCommentId = null
                                    },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom modern squarish input
                            Box(
                                modifier = Modifier.weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                BasicTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    modifier = Modifier.fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        if (commentText.isEmpty()) {
                                            Text(
                                                text = "Add a comment...",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    })
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        if (replyingToCommentId != null) {
                                            onReplyToComment(replyingToCommentId, commentText)
                                        } else {
                                            onSendComment(commentText)
                                        }
                                        commentText = ""
                                        replyingToUser = null
                                        replyingToCommentId = null
                                    }
                                },
                                enabled = commentText.isNotBlank(),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Post",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (actionState is ActionState.Loading && !isLoadingMore) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun StatisticsBar(
    statistics: PostStatsSerializers?,
    loadedCommentCount: Int,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier
) {
    val totalComments = statistics?.commentCount ?: loadedCommentCount
    val likeCount = statistics?.likeCount ?: 0
    val shareCount = statistics?.shareCount ?: 0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: comment count
        val displayText =
            if (statistics != null && totalComments > loadedCommentCount && isLoadingMore) {
                "$loadedCommentCount / $totalComments Comments"
            } else {
                "$totalComments Comments"
            }
        Text(
            text = displayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Right: likes and shares
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = likeCount.toString(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Share count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = shareCount.toString(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
