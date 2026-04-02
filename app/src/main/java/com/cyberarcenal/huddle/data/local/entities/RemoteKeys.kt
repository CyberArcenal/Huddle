// data/local/entities/RemoteKeys.kt
package com.cyberarcenal.huddle.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey val feedType: String,
    val nextKey: Int?,
    val prevKey: Int? = null,
    val lastUpdated: Long = System.currentTimeMillis()  // ← para malaman kung luma na
)