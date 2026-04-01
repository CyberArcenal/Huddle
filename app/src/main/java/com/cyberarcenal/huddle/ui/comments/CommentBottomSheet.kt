package com.cyberarcenal.huddle.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.CommentDisplay
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.data.reactionPicker.ReactionPickerLayout
import com.cyberarcenal.huddle.ui.common.managers.ActionState
import kotlinx.coroutines.launch
import kotlin.collections.get

data class CommentSheetState(
    val contentType: String,
    val objectId: Int,
    val statistics: PostStatsSerializers? = null  // NEW
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
    statistics: PostStatsSerializers? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    var commentText by remember { mutableStateOf("") }
    var replyingToUser by remember { mutableStateOf<String?>(null) }
    var replyingToCommentId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= comments.size - 3) {
                    onLoadMore()
                }
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        ReactionPickerLayout {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 650.dp)
                    .background(Color.White)
            ) {
                StatisticsBar(
                    statistics = statistics,
                    loadedCommentCount = comments.size,
                    isLoadingMore = isLoadingMore,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

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
                            Text(text = "No comments yet.", color = Color.Gray)
                        }
                    } else {

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
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
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (replyingToUser != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Replying to $replyingToUser",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        replyingToUser = null
                                        replyingToCommentId = null
                                    },
                                tint = Color.Gray
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Add a comment...", fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFFF5F5F5),
                                unfocusedContainerColor = Color(0xFFF5F5F5),
                                cursorColor = Color.Black
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )

                        if (commentText.isNotBlank()) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Post",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    if (replyingToCommentId != null) {
                                        onReplyToComment(replyingToCommentId, commentText)
                                    } else {
                                        onSendComment(commentText)
                                    }
                                    commentText = ""
                                    replyingToUser = null
                                    replyingToCommentId = null
                                }
                            )
                        }

                    }

                    if (actionState is ActionState.Loading && !isLoadingMore) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
        val displayText = if (statistics != null && totalComments > loadedCommentCount && isLoadingMore) {
            "$loadedCommentCount / $totalComments Comments"
        } else {
            "$totalComments Comments"
        }
        Text(
            text = displayText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        // Right: likes and shares
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like count
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Text(
                text = likeCount.toString(),
                fontSize = 14.sp,
                color = Color.Gray
            )
            // Share count
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Text(
                text = shareCount.toString(),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
