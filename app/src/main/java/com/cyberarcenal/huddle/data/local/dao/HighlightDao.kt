package com.cyberarcenal.huddle.data.local.dao

import androidx.room.*
import com.cyberarcenal.huddle.data.local.entities.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Query("SELECT * FROM user_highlights WHERE userId = :userId ORDER BY cachedAt DESC")
    fun observeHighlights(userId: Int): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(highlights: List<HighlightEntity>)

    @Query("DELETE FROM user_highlights WHERE userId = :userId")
    suspend fun clearHighlights(userId: Int)

    @Transaction
    suspend fun refreshHighlights(userId: Int, highlights: List<HighlightEntity>) {
        clearHighlights(userId)
        insertAll(highlights)
    }
}