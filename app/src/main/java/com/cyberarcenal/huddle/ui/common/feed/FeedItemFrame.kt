// FeedItemFrame.kt

package com.cyberarcenal.huddle.ui.common.feed

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCount
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.api.models.ShareFeed
import com.cyberarcenal.huddle.api.models.Story
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.ui.feed.safeConvertTo
import com.cyberarcenal.huddle.utils.formatRelativeTime
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
    caption: String? = null,
    onReactionClick: (ReactionType?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
    postData: Any? = null,
    showBottomDivider: Boolean = true
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

    // Sa loob ng FeedItemFrame component...

    var showShareSheet by remember { mutableStateOf(false) }
    val displayName = when {
        !user?.fullName.isNullOrBlank() -> user.fullName
        !user?.username.isNullOrBlank() -> user.username
        else -> "Unknown"
    }

    data class AnyPost(
        val id: Int?,
    )

    // Display Share Bottom Sheet
    if (showShareSheet) {
        val contentType: String?  = when (postData){is PostFeed -> "feed.post"; is ShareFeed ->
            "feed.share"; is EventList -> "event.event"; is Story -> "stories.story"; is
        ReelDisplay -> "feed.reel" else
            -> null}
        contentType?.let{
            val data = safeConvertTo<AnyPost>(postData!!, tag = "Convert Post Data to any")
            data?.let {
                ShareBottomSheet(
                    onDismiss = { showShareSheet = false },
                    onShare = { shareData ->
                        showShareSheet = false
                        // Ipasa ang shareData sa parent callback
                        onShareClick(shareData)
                        // Note: Maaari mong i-update ang onShareClick signature
                        // para tanggapin ang ShareRequestData kung kailangan.
                    },
                    contentType = "feed.post", // Default, maaari itong gawing dynamic
                    contentId = data.id ?: 0 // Siguraduhing may ID ang statistics o ipasa ito as param
                )
            }

        }

    }


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
                    text = displayName,
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

        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // --- CONTENT SLOT ---
        content()

        // --- REACTION SUMMARY (if any) ---
        ReactionSummary(reactionCount = statistics?.reactionCount)

        // --- INTERACTION BAR ---
        InteractionBar(
            currentReaction = localReaction,
            reactionCount = localTotalReactions,
            commentCount = commentCount,
            onReactionSelected = { newReaction ->
                handleReactionUpdate(newReaction)
            },
            onCommentClick = onCommentClick,
            onShareClick = {
                showShareSheet = true
            },
        )
        if (showBottomDivider) {
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
        }
    }
}