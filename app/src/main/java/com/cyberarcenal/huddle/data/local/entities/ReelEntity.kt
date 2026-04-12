package com.cyberarcenal.huddle.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyberarcenal.huddle.api.models.ReelDisplay

@Entity(tableName = "user_reels")
data class ReelEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val position: Int,
    val rawData: ReelDisplay
)
