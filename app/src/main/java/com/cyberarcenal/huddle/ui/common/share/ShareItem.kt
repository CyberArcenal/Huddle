package com.cyberarcenal.huddle.ui.common.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.event.EventItem
import com.cyberarcenal.huddle.ui.common.post.PostItem
import com.cyberarcenal.huddle.ui.common.reel.ReelFeedItem
import com.cyberarcenal.huddle.ui.common.reel.ReelsRowItemCard
import com.cyberarcenal.huddle.ui.common.userimage.UserImageFeedItem
import com.cyberarcenal.huddle.ui.feed.safeConvertTo

@Composable
fun ShareItem(
    shareFeed: ShareFeed,
    onImageClick: (MediaDetailData) -> Unit,
    onReelClick: (ReelDisplay) -> Unit,
    onEventClick: (EventList) -> Unit,
    onProfileClick: (Int) -> Unit,
    onVideoClick: (ReelDisplay) -> Unit
) {
    val originalContent = shareFeed.contentObjectData
    val type = shareFeed.contentObjectDetail?.type

    when (type) {
        "post" -> {
            val postData = safeConvertTo<PostFeed>(originalContent ?: return ContentRemovedPlaceholder(), tag = "post feed share")
            if (postData != null) {
                PostItem(post = postData, onImageClick = onImageClick)
            } else {
                ContentRemovedPlaceholder()
            }
        }
        "reel" ->  {
            val data = safeConvertTo<ReelDisplay>(originalContent ?: return ContentRemovedPlaceholder(), tag = "post feed share")
            if (data != null) {
                ReelFeedItem(
                    reel = data,
                    onReelClick = { onReelClick(data) },
                    onProfileClick = { data.user?.id?.let { onProfileClick(it)  } },
                    onVideoClick = {onVideoClick(data)},
                )
            } else {
                ContentRemovedPlaceholder()
            }
        }
        "userimage" -> {
            val data = safeConvertTo<UserImageDisplay>(originalContent ?: return ContentRemovedPlaceholder(), tag = "post feed share")
            if (data != null) {
                UserImageFeedItem(userImage = data, user = data.user, onImageClick = onImageClick)
            } else {
                ContentRemovedPlaceholder()
            }
        }
        "event" ->  {
            val data = safeConvertTo<EventList>(originalContent ?: return ContentRemovedPlaceholder(), tag = "post feed share")
            if (data != null) {
                EventItem(
                    event = data, isPostLike = true,
                    onItemClick = { onEventClick(data) }
                )
            } else {
                ContentRemovedPlaceholder()
            }
        }
        else -> {
            ContentRemovedPlaceholder()
        }
    }
}

@Composable
fun ContentRemovedPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This content is no longer available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = "The original post may have been deleted or its privacy settings changed.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
