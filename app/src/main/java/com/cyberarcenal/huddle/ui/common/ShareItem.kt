package com.cyberarcenal.huddle.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.EventList
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.api.models.ShareFeed

@Composable
fun ShareItem(
    share: ShareFeed,
    onProfileClick: (Int) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onReactionClick: (ReactionType?) -> Unit = {},
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    FeedItemFrame(
        user = share.user,
        createdAt = share.createdAt,
        statistics = share.statistics as PostStatsSerializers,
        headerSuffix = "shared a post",
        onReactionClick = onReactionClick,
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
