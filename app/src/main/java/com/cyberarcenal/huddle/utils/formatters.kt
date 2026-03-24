package com.cyberarcenal.huddle.utils

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