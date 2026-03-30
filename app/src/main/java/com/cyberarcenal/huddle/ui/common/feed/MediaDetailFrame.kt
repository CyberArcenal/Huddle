package com.cyberarcenal.huddle.ui.common.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.utils.formatRelativeTime
import java.time.OffsetDateTime

@Composable
fun MediaDetailFrame(
    user: UserMinimal?,
    createdAt: Any?,
    statistics: PostStatsSerializers?,
    onCloseClick: () -> Unit,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    // Kinukuha natin ang parehong reaction logic mula sa iyong FeedItemFrame
    val commentCount = statistics?.commentCount ?: 0
    val currentReactionFromServer = mapCurrentReaction(statistics?.currentReaction)
    val totalReactionsFromServer = getTotalReactionCount(statistics?.reactionCount)

    var localReaction by remember { mutableStateOf(currentReactionFromServer) }
    var localTotalReactions by remember { mutableIntStateOf(totalReactionsFromServer) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fullscreen black background
    ) {
        // --- 1. MAIN MEDIA CONTENT (Ang Image/Video) ---
        content()

        // --- 2. TOP BAR (Close Button & More) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            IconButton(
                onClick = { /* More options */ },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
        }

        // --- 3. BOTTOM INFO & INTERACTION (Overlay) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // User Info Header (Simplified for Detail View)
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(url = user?.profilePictureUrl, username = user?.username)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user?.username ?: "Unknown",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Posted • ${if (createdAt is OffsetDateTime) formatRelativeTime(createdAt) else createdAt}",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Interaction Bar (White themed for dark background)
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                InteractionBar(
                    currentReaction = localReaction,
                    reactionCount = localTotalReactions,
                    commentCount = commentCount,
                    onReactionSelected = { new ->
                        localReaction = new
                        onReactionClick(new)
                    },
                    onCommentClick = onCommentClick,
                    onShareClick = onShareClick
                )
            }
        }
    }
}