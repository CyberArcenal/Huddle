package com.cyberarcenal.huddle.ui.dating

import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    fun toRelativeTime(timestamp: OffsetDateTime?): String {
        if (timestamp == null) return ""
        val now = OffsetDateTime.now(ZoneId.systemDefault())
        val duration = Duration.between(timestamp, now)

        return when {
            duration.seconds < 60 -> "just now"
            duration.seconds < 3600 -> "${duration.toMinutes()} min ago"
            duration.seconds < 86400 -> "${duration.toHours()} hr ago"
            duration.seconds < 604800 -> "${duration.toDays()} d ago"
            else -> timestamp.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
}