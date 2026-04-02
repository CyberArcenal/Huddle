package com.cyberarcenal.huddle.data.local.dao

import androidx.room.*
import com.cyberarcenal.huddle.data.local.entities.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :userId")
    fun getProfile(userId: Int): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :userId")
    suspend fun deleteProfile(userId: Int)
}
