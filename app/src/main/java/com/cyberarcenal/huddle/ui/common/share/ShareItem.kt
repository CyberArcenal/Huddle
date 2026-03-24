package com.cyberarcenal.huddle.ui.common.share

import androidx.compose.runtime.Composable
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.event.EventItem
import com.cyberarcenal.huddle.ui.common.post.PostItem
import com.cyberarcenal.huddle.ui.common.reel.ReelsItemCard
import com.cyberarcenal.huddle.ui.common.userimage.UserImageFeedItem
import com.cyberarcenal.huddle.ui.feed.safeConvertTo

@Composable
fun ShareItem(
    shareFeed: ShareFeed,
    onImageClick: (MediaDetailData) -> Unit = {},
    onReelClick: (ReelDisplay) -> Unit = {},
    onEventClick: (EventList) -> Unit = {},
) {
    val originalContent = shareFeed.contentObjectData;
    when (shareFeed.contentObjectDetail?.type) {
        "post" -> {
            val postData = safeConvertTo<PostFeed>(originalContent!!, tag = "post feed share");
            postData?.let {
                PostItem(post = postData, onImageClick = onImageClick)
            }
        }
        "reel" ->  {
            val data = safeConvertTo<ReelDisplay>(originalContent!!, tag = "post feed share");
            data?.let {
                ReelsItemCard(
                    reel = it,
                    onClick = {},
                )
            }
        }
        "userimage" -> {
            val data = safeConvertTo<UserImageDisplay>(originalContent!!, tag = "post feed share");
            data?.let {
                UserImageFeedItem(userImage = it, user = it.user, onImageClick =
                    onImageClick)
            }

        }
        "event" ->  {
            val data = safeConvertTo<EventList>(originalContent!!, tag = "post feed share");
            data?.let {
                EventItem(
                    event = it, isVertical = false,
                    onItemClick = {onEventClick(it)}
                )
            }

        }
    }
}