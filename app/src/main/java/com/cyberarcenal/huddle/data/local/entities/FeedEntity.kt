// data/local/entities/FeedEntity.kt
package com.cyberarcenal.huddle.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyberarcenal.huddle.api.models.UnifiedContentItem

@Entity(tableName = "feed_items")
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val feedType: String,          // "HOME", "DISCOVER", etc.
    val page: Int,                 // ← bagong field (para sa pagination)
    val position: Int,             // posisyon sa loob ng page
    val rawData: UnifiedContentItem,
    val cachedAt: Long = System.currentTimeMillis()  // ← para sa TTL
)