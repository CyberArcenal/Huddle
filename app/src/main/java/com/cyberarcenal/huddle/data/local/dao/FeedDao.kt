package com.cyberarcenal.huddle.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.cyberarcenal.huddle.data.local.entities.FeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {

    @Query("SELECT * FROM feed_items WHERE feedType = :feedType ORDER BY page ASC, position ASC")
    fun observeFeed(feedType: String): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feed_items WHERE feedType = :feedType ORDER BY page ASC, position ASC")
    fun getPagingSource(feedType: String): PagingSource<Int, FeedEntity>

    @Query("SELECT * FROM feed_items WHERE feedType = :feedType AND page = :page ORDER BY position ASC")
    suspend fun getFeedPage(feedType: String, page: Int): List<FeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FeedEntity>)

    @Query("DELETE FROM feed_items WHERE feedType = :feedType AND page = :page")
    suspend fun clearPage(feedType: String, page: Int)

    @Query("DELETE FROM feed_items WHERE feedType = :feedType")
    suspend fun clearFeed(feedType: String)

    @Query("DELETE FROM feed_items WHERE feedType = :feedType AND cachedAt < :expiryTime")
    suspend fun deleteExpired(feedType: String, expiryTime: Long)

    @Transaction
    suspend fun refreshPage(feedType: String, page: Int, items: List<FeedEntity>) {
        clearPage(feedType, page)
        insertAll(items)
    }
}
