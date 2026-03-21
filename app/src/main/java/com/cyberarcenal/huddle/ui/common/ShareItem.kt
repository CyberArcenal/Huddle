package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionResponse
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.api.models.ShareFeed
import com.cyberarcenal.huddle.ui.common.FeedItemFrame

@Composable
fun ShareItem(
    share: ShareFeed,
    onProfileClick: (Int) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onReactionClick: (ReactionResponse.ReactionType) -> Unit = {},
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    FeedItemFrame(
        user = share.user,
        createdAt = share.createdAt,
        statistics = share.statistics as PostStatsSerializers,
        headerSuffix = "shared a post",
        onReactionClick = {onReactionClick(it as ReactionResponse.ReactionType)},
        onCommentClick = onCommentClick,
        onShareClick = onShareClick,
        onMoreClick = onMoreClick,
        onProfileClick = onProfileClick,
        content = {
            Column {
                if (!share.caption.isNullOrBlank()) {
                    Text(
                        text = share.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                share.contentObject?.let { original ->
                    when (original) {
                        is PostFeed -> PostItem(post = original, onImageClick = onImageClick)
                        is EventList -> EventItem(event = original, isVertical = false) { /* navigate */ }
                        is ReelDisplay -> ReelsItemCard(reel = original) { /* navigate */ }
                        else -> {} // fallback
                    }
                }
            }
        }
    )
}
