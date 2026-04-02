// data/local/dao/RemoteKeysDao.kt
package com.cyberarcenal.huddle.data.local.dao

import androidx.room.*
import com.cyberarcenal.huddle.data.local.entities.RemoteKeys

@Dao
interface RemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(remoteKey: RemoteKeys)

    @Query("SELECT * FROM remote_keys WHERE feedType = :feedType")
    suspend fun remoteKeysByFeedType(feedType: String): RemoteKeys?

    @Query("DELETE FROM remote_keys WHERE feedType = :feedType")
    suspend fun deleteByFeedType(feedType: String)

    // Update ng lastUpdated nang hindi binabago ang ibang fields
    @Query("UPDATE remote_keys SET lastUpdated = :time WHERE feedType = :feedType")
    suspend fun updateLastUpdated(feedType: String, time: Long)
}