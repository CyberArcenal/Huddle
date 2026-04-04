package com.cyberarcenal.huddle.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyberarcenal.huddle.api.models.UserProfile

// ProfileEntity.kt
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: Int,
    val username: String?,
    val profilePictureUrl: String?,
    val coverPhotoUrl: String?,
    val bio: String?,
    val rawData: UserProfile?,
    val lastUpdated: Long = System.currentTimeMillis()   // new field
)
