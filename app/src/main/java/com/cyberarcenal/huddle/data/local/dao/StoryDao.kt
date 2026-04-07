package com.cyberarcenal.huddle.data.local.dao

import androidx.room.*
import com.cyberarcenal.huddle.data.local.entities.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Query("SELECT * FROM story_items WHERE storyType = :storyType ORDER BY position ASC")
    fun observeStories(storyType: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM story_items WHERE storyType = :storyType ORDER BY position ASC")
    suspend fun getStories(storyType: String): List<StoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StoryEntity>)

    @Query("DELETE FROM story_items WHERE storyType = :storyType")
    suspend fun clearStories(storyType: String)

    @Transaction
    suspend fun refreshStories(storyType: String, items: List<StoryEntity>) {
        clearStories(storyType)
        insertAll(items)
    }
}