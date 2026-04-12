package com.cyberarcenal.huddle.data.local.dao

import androidx.room.*
import com.cyberarcenal.huddle.data.local.entities.ReelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {
    @Query("SELECT * FROM user_reels WHERE userId = :userId ORDER BY position ASC")
    fun observeReels(userId: Int): Flow<List<ReelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reels: List<ReelEntity>)

    @Query("DELETE FROM user_reels WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Int)

    @Transaction
    suspend fun refreshReels(userId: Int, reels: List<ReelEntity>) {
        deleteByUserId(userId)
        insertAll(reels)
    }
}
