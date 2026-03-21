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
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.ui.feed.ActionState
import kotlinx.coroutines.launch
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    postId: Int,
    comments: List<CommentDisplay>,
    replies: Map<Int, List<CommentDisplay>>,
    expandedReplies: Set<Int>,
    currentUserId: Int?,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onToggleReplyExpanded: (Int?) -> Unit,
    onLoadReplies: (Int?) -> Unit,
    onReactToComment: (Int, ReactionCreateRequest.ReactionType?) -> Unit,  // changed
    onReplyToComment: (Int?, String) -> Unit,
    onReportComment: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit,
    onDeleteComment: (Int) -> Unit,
    actionState: ActionState,
    errorMessage: String?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    var commentText by remember { mutableStateOf("") }
    var replyingToUser by remember { mutableStateOf<String?>(null) }
    var replyingToCommentId by remember { mutableStateOf<Int?>(null) }

    // Detect when near end to load more
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp)
                .background(Color.White)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            // Comments list
            Box(modifier = Modifier.weight(1f)) {
                if (errorMessage != null && comments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                } else if (comments.isEmpty() && !isLoadingMore) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No comments yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(comments) { comment ->
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
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
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
                // Replying to indicator
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
            }

            if (actionState is ActionState.Loading && !isLoadingMore) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
