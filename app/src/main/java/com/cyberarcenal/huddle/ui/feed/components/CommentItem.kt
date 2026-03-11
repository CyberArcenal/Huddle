package com.cyberarcenal.huddle.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.Comment

@Composable
fun CommentItem(
    comment: Comment,
    replies: List<Comment>,
    isExpanded: Boolean,
    currentUserId: Int?,
    onToggleExpand: () -> Unit,
    onLike: () -> Unit,
    onReply: (String) -> Unit,
    onReport: () -> Unit,
    level: Int
) {
    var showOptions by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var showReplyInput by remember { mutableStateOf(false) }

    val isOwnComment = comment.user?.id == currentUserId

    Row(modifier = Modifier.fillMaxWidth()) {
        // Thread line for replies (level > 0)
        if (level > 0) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
                    .padding(start = 8.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (level > 0) 4.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .combinedClickable(
                        onClick = { /* maybe nothing */ },
                        onLongClick = { showOptions = true }
                    ),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = if (isOwnComment) Arrangement.End else Arrangement.Start
            ) {
                // Avatar for others (left side)
                if (!isOwnComment) {
                    Avatar(comment.user?.profilePictureUrl, comment.user?.username)
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Comment bubble (wraps content)
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isOwnComment) 16.dp else 4.dp,
                        topEnd = if (isOwnComment) 4.dp else 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = 16.dp
                    ),
                    color = if (isOwnComment)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.wrapContentWidth() // Hindi na pumupuno sa buong width
                        ) {
                            Text(
                                text = comment.user?.username ?: "Unknown",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (isOwnComment)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatRelativeTime(comment.createdAt),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = comment.content,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Avatar for own comment (right side)
                if (isOwnComment) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Avatar(comment.user?.profilePictureUrl, comment.user?.username)
                }
            }

            // Options dialog (unchanged)
            if (showOptions) {
                AlertDialog(
                    onDismissRequest = { showOptions = false },
                    title = { Text("Comment Options") },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showOptions = false
                                        onLike()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (comment.hasLiked == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (comment.hasLiked == true) Color.Red else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (comment.hasLiked == true) "Unlike" else "Like")
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showOptions = false
                                        showReplyInput = true
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Reply, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reply")
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showOptions = false
                                        onReport()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Report", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showOptions = false }) { Text("Cancel") } }
                )
            }

            // Like and reply buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isOwnComment) 0.dp else 44.dp,
                        end = if (isOwnComment) 44.dp else 0.dp,
                        top = 4.dp
                    ),
                horizontalArrangement = if (isOwnComment) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLike() }
                ) {
                    Icon(
                        if (comment.hasLiked == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(16.dp),
                        tint = if (comment.hasLiked == true) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (comment.likeCount ?: 0 > 0) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = comment.likeCount.toString(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showReplyInput = !showReplyInput }
                ) {
                    Icon(
                        Icons.Default.Reply,
                        contentDescription = "Reply",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (replies.isNotEmpty()) "Reply (${replies.size})" else "Reply",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reply input field
            if (showReplyInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (level > 0) 8.dp else 8.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a reply...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReply(replyText)
                                replyText = ""
                                showReplyInput = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Expandable replies
            if (isExpanded && replies.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    replies.forEach { reply ->
                        CommentItem(
                            comment = reply,
                            replies = emptyList(),
                            isExpanded = false,
                            currentUserId = currentUserId,
                            onToggleExpand = {},
                            onLike = { /* handle like for reply */ },
                            onReply = { /* handle reply to reply? maybe not needed */ },
                            onReport = { /* handle report for reply */ },
                            level = level + 1
                        )
                    }
                }
            }

            // Expand/collapse indicator
            if (replies.isNotEmpty()) {
                TextButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.padding(start = (level * 16 + 8).dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide replies" else "View replies (${replies.size})",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}