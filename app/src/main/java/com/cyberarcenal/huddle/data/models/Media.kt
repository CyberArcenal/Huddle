package com.cyberarcenal.huddle.data.models

import com.cyberarcenal.huddle.api.models.MediaDisplay
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.UserMinimal
import java.time.OffsetDateTime

data class MediaDetailData(
    val url: String,
    val user: UserMinimal?,
    val createdAt: OffsetDateTime?,
    val stats: PostStatsSerializers?,
    val id: Int,
    val type: String,
    val allMedia: List<MediaDisplay>? = null,
    val initialIndex: Int = 0
)
