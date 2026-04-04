package com.cyberarcenal.huddle.data.local.dao

import androidx.room.*
import com.cyberarcenal.huddle.data.local.entities.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :userId")
    fun getProfile(userId: Int): Flow<ProfileEntity?>

    // One‑time fetch (for cache validation)
    @Query("SELECT * FROM profiles WHERE id = :userId")
    suspend fun getProfileSync(userId: Int): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :userId")
    suspend fun deleteProfile(userId: Int)

    // Optional: manual timestamp update if you ever need it
    @Query("UPDATE profiles SET lastUpdated = :time WHERE id = :userId")
    suspend fun updateLastUpdated(userId: Int, time: Long)
}
