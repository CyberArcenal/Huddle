package com.cyberarcenal.huddle.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyberarcenal.huddle.api.models.StoryFeed

@Entity(tableName = "story_items")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val storyType: String, // "FOLLOWING", "POPULAR", etc.
    val position: Int,
    val rawData: StoryFeed,
    val cachedAt: Long = System.currentTimeMillis()
)