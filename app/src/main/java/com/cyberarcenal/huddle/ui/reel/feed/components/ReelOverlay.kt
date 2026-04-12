package com.cyberarcenal.huddle.ui.reel.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.R
import com.cyberarcenal.huddle.api.models.ReactionTypeEnum
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.data.models.Reaction
import com.cyberarcenal.huddle.data.reactionPicker.reactionPickerAnchor
import com.cyberarcenal.huddle.data.reactionPicker.rememberReactionPickerState
import com.cyberarcenal.huddle.ui.common.feed.getReactionIcon
import com.cyberarcenal.huddle.ui.common.user.Avatar

@Composable
fun ReelOverlay(
    currentUserId: Int?,
    reel: ReelDisplay,
    onReactionClick: (ReactionTypeEnum?) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: (Int?) -> Unit,
    onFollowClick: (Int, Boolean, String) -> Unit = { _, _, _ -> },
    onCreateClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    val statistics = reel.statistics
    val user = reel.user
    val userId = user?.id

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

    val currentReactionFromServer = mapCurrentReaction(statistics?.currentReaction)
    val totalLikesFromServer = statistics?.likeCount ?: 0

    var localReaction by remember { mutableStateOf(currentReactionFromServer) }
    var localLikeCount by remember { mutableIntStateOf(totalLikesFromServer) }

    LaunchedEffect(statistics) {
        localReaction = currentReactionFromServer
        localLikeCount = totalLikesFromServer
    }

    val handleReactionUpdate = { newReaction: ReactionTypeEnum? ->
        val hadReaction = localReaction != null
        val willHaveReaction = newReaction != null

        if (!hadReaction && willHaveReaction) {
            localLikeCount++
        } else if (hadReaction && !willHaveReaction) {
            localLikeCount--
        }
        localReaction = newReaction
        onReactionClick(newReaction)
    }

    val reactionItems = remember {
        listOf(
            Reaction(key = ReactionTypeEnum.LIKE, label = "Like", painterResource = R.drawable.like),
            Reaction(key = ReactionTypeEnum.DISLIKE, label = "Dislike", painterResource = R
                .drawable.dislike),
            Reaction(key = ReactionTypeEnum.LOVE, label = "Love", painterResource = R.drawable.love),
            Reaction(key = ReactionTypeEnum.CARE, label = "Care", painterResource = R.drawable.care),
            Reaction(key = ReactionTypeEnum.HAHA, label = "Haha", painterResource = R.drawable.haha),
            Reaction(key = ReactionTypeEnum.WOW, label = "Wow", painterResource = R.drawable.wow),
            Reaction(key = ReactionTypeEnum.SAD, label = "Sad", painterResource = R.drawable.sad),
            Reaction(key = ReactionTypeEnum.ANGRY, label = "Angry", painterResource = R.drawable.angry),
        )
    }

    val pickerState = rememberReactionPickerState(
        reactions = reactionItems,
        initialSelection = reactionItems.find { it.key == localReaction }
    )

    LaunchedEffect(pickerState.selectedReaction) {
        val selectedKey = pickerState.selectedReaction?.key as? ReactionTypeEnum
        if (selectedKey != localReaction) {
            handleReactionUpdate(selectedKey)
        }
    }

    statistics?.let {
        Box(modifier = Modifier.fillMaxSize()) {
            // Bottom Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Right side action buttons
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val (iconData, tint) = getReactionIcon(localReaction)

                ReelActionButton(
                    icon = Icons.Default.AddCircle,
                    label = "Create",
                    tint = Color.White,
                    onClick = onCreateClick
                )
                
                ReelActionButton(
                    icon = iconData,
                    label = "$localLikeCount",
                    modifier = Modifier.reactionPickerAnchor(pickerState),
                    tint = if (localReaction != null) Color.Unspecified else Color.White,
                    onClick = {
                        val next = if (localReaction != null) null else ReactionTypeEnum.LIKE
                        handleReactionUpdate(next)
                    }
                )

                ReelActionButton(
                    icon = R.drawable.comment,
                    label = "${it.commentCount ?: 0}",
                    tint = Color.White,
                    onClick = onCommentClick
                )

                ReelActionButton(
                    icon = R.drawable.share_ios,
                    label = "Share",
                    tint = Color.White,
                    onClick = onShareClick
                )

                ReelActionButton(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    tint = Color.White,
                    onClick = onMoreClick
                )

                // User Avatar
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick(userId) }
                    ) {
                        Avatar(
                            url = user?.profilePictureUrl,
                            username = user?.username ?: "",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }

            // Bottom info

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 20.dp, end = 80.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onProfileClick(userId) }
                ) {
                    Text(
                        text = "${user?.fullName ?: user?.username}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    user?.let { user ->
                        if (user.id?.equals(currentUserId) == false){
                            if (user.isFollowing == true){
                                Text(
                                    text = "Following",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable {
                                        onFollowClick(user.id ?: 0, true, user.username ?: "")
                                    }
                                )
                            }else{
                                Text(
                                    text = "Follow",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable {
                                        onFollowClick(user.id ?: 0, false, user.username ?: "")
                                    }
                                )
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!reel.caption.isNullOrBlank()) {
                    Text(
                        text = reel.caption,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Original Audio - ${user?.username}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

