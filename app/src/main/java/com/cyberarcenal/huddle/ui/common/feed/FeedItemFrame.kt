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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.FeelingEnum
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.PrivacyB23Enum
import com.cyberarcenal.huddle.api.models.ReactionCount
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Lock
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
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

// Helper: map currentReaction string to ReactionTypeEnum?
fun mapCurrentReaction(currentReaction: String?): ReactionTypeEnum? {
    return when (currentReaction?.lowercase()) {
        "like" -> ReactionTypeEnum.LIKE
        "dislike" -> ReactionTypeEnum.DISLIKE
        "love" -> ReactionTypeEnum.LOVE
        "care" -> ReactionTypeEnum.CARE
        "haha" -> ReactionTypeEnum.HAHA
        "wow" -> ReactionTypeEnum.WOW
        "sad" -> ReactionTypeEnum.SAD
        "angry" -> ReactionTypeEnum.ANGRY
        else -> null
    }
}

// Helper: get total reaction count from ReactionCount object
fun getTotalReactionCount(reactionCount: ReactionCount?): Int {
    return (reactionCount?.like ?: 0) +
            (reactionCount?.dislike ?:0)+
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
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onMoreClick: () -> Unit = {},
    onProfileClick: (Int) -> Unit = {},
    onGroupClick: (Int) -> Unit = {},
    onReactionSummaryClick: () -> Unit = onCommentClick,
    onCommentSummaryClick: () -> Unit = onCommentClick,
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
    val handleReactionUpdate = { newReaction: ReactionTypeEnum? ->
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
            Reaction(key = ReactionTypeEnum.LIKE, label = "Like", imageVector = Icons.Filled.ThumbUp),
            Reaction(key = ReactionTypeEnum.DISLIKE, label = "dislike", imageVector = Icons.Filled.ThumbUp),
            Reaction(key = ReactionTypeEnum.LOVE, label = "Love", imageVector = Icons.Filled.Favorite),
            Reaction(key = ReactionTypeEnum.CARE, label = "Care", imageVector = Icons.Filled.Favorite),
            Reaction(key = ReactionTypeEnum.HAHA, label = "Haha", imageVector = Icons.Filled.SentimentSatisfiedAlt),
            Reaction(key = ReactionTypeEnum.WOW, label = "Wow", imageVector = Icons.Filled.SentimentVerySatisfied),
            Reaction(key = ReactionTypeEnum.SAD, label = "Sad", imageVector = Icons.Filled.SentimentDissatisfied),
            Reaction(key = ReactionTypeEnum.ANGRY, label = "Angry", imageVector = Icons.Filled.SentimentVeryDissatisfied)
        )
    }

    val reactionState = rememberReactionPickerState(
        reactions = reactions,
        initialSelection = reactions.find { it.key == localReaction },
        onReacted = { reaction ->
            val selectedType = reaction?.key as? ReactionTypeEnum
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

    val group = when (postData) {
        is PostFeed -> postData.group
        is ShareFeed -> postData.group
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { user?.id?.let { onProfileClick(it) } }
                    )

                    if (postData is PostFeed) {
                        val feeling = postData.feeling
                        val location = postData.location
                        val tagUsers = postData.tagUsers

                        if (feeling != null) {
                            val emoji = when (feeling) {
                                FeelingEnum.HAPPY -> "😊"
                                FeelingEnum.SAD -> "😢"
                                FeelingEnum.LOVE -> "🥰"
                                FeelingEnum.CRAZY -> "🤪"
                                FeelingEnum.COOL -> "😎"
                                FeelingEnum.EXCITED -> "🤩"
                                FeelingEnum.ANGRY -> "😠"
                                FeelingEnum.BORED -> "😑"
                                FeelingEnum.TIRED -> "😴"
                                FeelingEnum.CONFUSED -> "😕"
                                FeelingEnum.ANXIOUS -> "😰"
                                FeelingEnum.PROUD -> "😤"
                                FeelingEnum.LONELY -> "😔"
                                FeelingEnum.BLESSED -> "😇"
                            }
                            val feelingLabel = feeling.value.replaceFirstChar { it.uppercase() }
                            Text(
                                text = " is feeling $feelingLabel $emoji",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        
                        if (!location.isNullOrBlank()) {
                             Text(
                                text = " at $location",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        if (!tagUsers.isNullOrEmpty()) {
                            val tagText = " with " + tagUsers.joinToString(", ") { 
                                it.fullName ?: it.username ?: ""
                            }
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    if (group != null) {
                        Text(
                            text = " \u25B8 ",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Text(
                            text = group.name ?: "Unknown Group",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { group.id?.let { onGroupClick(it) } }
                        )
                    }
                }

                val timeLabel = when (createdAt) {
                    is OffsetDateTime -> formatRelativeTime(createdAt)
                    is String -> createdAt
                    else -> ""
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$timeLabel $headerSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val privacy = when (postData) {
                        is PostFeed -> postData.privacy
                        else -> null
                    }
                    
                    if (privacy != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val privacyIcon = when (privacy) {
                            PrivacyB23Enum.PUBLIC -> Icons.Outlined.Public
                            PrivacyB23Enum.FOLLOWERS -> Icons.Outlined.People
                            PrivacyB23Enum.SECRET -> Icons.Outlined.Lock
                        }
                        Icon(
                            imageVector = privacyIcon,
                            contentDescription = privacy.name,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // --- CONTENT SLOT ---
        content()

        // --- REACTION SUMMARY (if any) ---
        ReactionSummary(
            statistics = statistics,
            onReactionSummaryClick = onReactionSummaryClick,
            onCommentSummaryClick = onCommentSummaryClick
        )

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
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}
