package com.cyberarcenal.huddle.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.ui.common.shimmer.shimmerEffect
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Nagbabalik ng string representation ng lumipas na oras (hal. 2m, 5h, 3d).
 */
fun formatRelativeTime(dateTime: OffsetDateTime?): String {
    if (dateTime == null) return ""

    val now = OffsetDateTime.now(ZoneId.systemDefault())
    val seconds = ChronoUnit.SECONDS.between(dateTime, now)
    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    val hours = ChronoUnit.HOURS.between(dateTime, now)
    val days = ChronoUnit.DAYS.between(dateTime, now)
    val weeks = ChronoUnit.WEEKS.between(dateTime, now)

    return when {
        seconds < 60 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${weeks}w"
    }
}






fun formatRelativeDate(dateTime: OffsetDateTime?): String {
    if (dateTime == null) return ""
    val now = OffsetDateTime.now()
    val diff = now.toInstant().toEpochMilli() - dateTime.toInstant().toEpochMilli()
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> "${days / 7} weeks ago"
    }
}