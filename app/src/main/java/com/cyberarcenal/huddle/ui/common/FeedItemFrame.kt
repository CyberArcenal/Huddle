// FeedItemFrame.kt

package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCount
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.comments.formatRelativeTime
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import java.time.OffsetDateTime

// Helper: map currentReaction string to ReactionType?
fun mapCurrentReaction(currentReaction: String?): ReactionType? {
    return when (currentReaction?.lowercase()) {
        "like" -> ReactionType.LIKE
        "love" -> ReactionType.LOVE
        "care" -> ReactionType.CARE
        "haha" -> ReactionType.HAHA
        "wow" -> ReactionType.WOW
        "sad" -> ReactionType.SAD
        "angry" -> ReactionType.ANGRY
        else -> null
    }
}

// Helper: get total reaction count from ReactionCount object
fun getTotalReactionCount(reactionCount: ReactionCount?): Int {
    return (reactionCount?.like ?: 0) +
            (reactionCount?.love ?: 0) +
            (reactionCount?.care ?: 0) +
            (reactionCount?.haha ?: 0) +
            (reactionCount?.wow ?: 0) +
            (reactionCount?.sad ?: 0) +
            (reactionCount?.angry ?: 0)
}

@Composable
fun FeedItemFrame(
    user: UserMinimal?,
    createdAt: Any?, // Can be String or OffsetDateTime
    statistics: PostStatsSerializers?,
    headerSuffix: String = "",
    onReactionClick: (ReactionType?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val commentCount = statistics?.commentCount ?: 0
    val currentReactionString = statistics?.currentReaction
    val reactionCountObject = statistics?.reactionCount

    val currentReactionFromServer = mapCurrentReaction(currentReactionString)
    val totalReactionsFromServer = getTotalReactionCount(reactionCountObject)

    var localReaction by remember { mutableStateOf(currentReactionFromServer) }
    var localTotalReactions by remember { mutableIntStateOf(totalReactionsFromServer) }
    var reactionButtonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(statistics) {
        localReaction = currentReactionFromServer
        localTotalReactions = totalReactionsFromServer
    }

    // Consolidated reaction handler
    val handleReactionUpdate = { newReaction: ReactionType? ->
        val hadReaction = localReaction != null
        val willHaveReaction = newReaction != null

        if (!hadReaction && willHaveReaction) {
            localTotalReactions++
        } else if (hadReaction && !willHaveReaction) {
            localTotalReactions--
        }
        localReaction = newReaction
        onReactionClick(newReaction)
    }

    val reactions = remember {
        listOf(
            Reaction(key = ReactionType.LIKE, label = "Like", imageVector = Icons.Filled.ThumbUp),
            Reaction(key = ReactionType.LOVE, label = "Love", imageVector = Icons.Filled.Favorite),
            Reaction(key = ReactionType.CARE, label = "Care", imageVector = Icons.Filled.Favorite),
            Reaction(key = ReactionType.HAHA, label = "Haha", imageVector = Icons.Filled.SentimentSatisfiedAlt),
            Reaction(key = ReactionType.WOW, label = "Wow", imageVector = Icons.Filled.SentimentVerySatisfied),
            Reaction(key = ReactionType.SAD, label = "Sad", imageVector = Icons.Filled.SentimentDissatisfied),
            Reaction(key = ReactionType.ANGRY, label = "Angry", imageVector = Icons.Filled.SentimentVeryDissatisfied)
        )
    }

    val reactionState = rememberReactionPickerState(
        reactions = reactions,
        initialSelection = reactions.find { it.key == localReaction },
        onReacted = { reaction ->
            val selectedType = reaction?.key as? ReactionType
            handleReactionUpdate(selectedType)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 4.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable { user?.id?.let { onProfileClick(it) } }) {
                Avatar(url = user?.profilePictureUrl, username = user?.fullName)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.username ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val timeLabel = when (createdAt) {
                    is OffsetDateTime -> formatRelativeTime(createdAt)
                    is String -> createdAt
                    else -> ""
                }
                Text(
                    text = "$timeLabel $headerSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
            }
        }

        // --- CONTENT SLOT ---
        content()

        // --- INTERACTION BAR ---
        InteractionBar(
            currentReaction = localReaction,
            reactionCount = localTotalReactions,
            commentCount = commentCount,
            onReactionSelected = { newReaction ->
                handleReactionUpdate(newReaction)
            },
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
        )
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
    }
}
