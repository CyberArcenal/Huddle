package com.cyberarcenal.huddle.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyberarcenal.huddle.api.models.StoryHighlight

@Entity(tableName = "user_highlights")
data class HighlightEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val title: String?,
    val rawData: StoryHighlight,
    val cachedAt: Long = System.currentTimeMillis()
)