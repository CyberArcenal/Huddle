package com.cyberarcenal.huddle.ui.common.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.user.Avatar
import com.cyberarcenal.huddle.ui.feed.safeConvertTo
import com.cyberarcenal.huddle.utils.formatRelativeTime
import java.time.OffsetDateTime

data class CaptionData(
    val caption: String? = null,
    val content: String? = null,
)

@Composable
fun ShareFrame(
    shareFeed: ShareFeed,
    headerSuffix: String = "",
    onImageClick: (MediaDetailData) -> Unit = {},
    content: @Composable (ColumnScope.() -> Unit),
    isPaused: Boolean,
) {
    val originalContent = shareFeed.contentObjectData
    val originalContentDetail = shareFeed.contentObjectDetail;
    val data = safeConvertTo<CaptionData>(originalContent!!, tag = "transfer to contentDta");
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) // theme border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 10.dp)
        ) {
            // Original author header (same style as FeedItemFrame)
            when (originalContentDetail?.type) {
                "post" -> {
                    val postData =
                        safeConvertTo<PostFeed>(originalContent, tag = "post feed share");
                    val originalUser = postData?.user
                    originalUser?.let {
                        ShareHeader(
                            profilePictureUrl = originalUser.profilePictureUrl,
                            fullName = originalUser.fullName,
                            createdAt = postData.createdAt,
                            headerSuffix = headerSuffix
                        )
                    }
                }
                "reel" -> {}
                "event" -> {}
                "userimage" -> {}
            }

            data?.caption?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 18.sp,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface, // theme text
                        modifier = Modifier
                            .padding(bottom = 8.dp, top = 8.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }
            data?.content?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 18.sp,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface, // theme text
                        modifier = Modifier
                            .padding(bottom = 8.dp, top = 8.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }
            content();
        }
    }
}

@Composable
fun ShareHeader(
    profilePictureUrl: String?,
    fullName: String?,
    createdAt: OffsetDateTime?,
    headerSuffix: String = "",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 5.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            url = profilePictureUrl,
            username = fullName,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Column {
            Text(
                text = fullName ?: "Unknown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // theme
            )
            val timeLabel = when (createdAt) {
                is OffsetDateTime -> formatRelativeTime(createdAt)
                is String -> createdAt
                else -> ""
            }
            Text(
                text = "$timeLabel $headerSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant // theme
            )
        }
    }
}