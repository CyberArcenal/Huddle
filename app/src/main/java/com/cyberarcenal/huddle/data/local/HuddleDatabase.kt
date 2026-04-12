package com.cyberarcenal.huddle.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.cyberarcenal.huddle.data.local.dao.*
import com.cyberarcenal.huddle.data.local.entities.*

@Database(
    entities = [ProfileEntity::class, FeedEntity::class, RemoteKeys::class, StoryEntity::class, HighlightEntity::class, ReelEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HuddleDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun feedDao(): FeedDao
    abstract fun remoteKeysDao(): RemoteKeysDao
    abstract fun storyDao(): StoryDao
    abstract fun highlightDao(): HighlightDao
    abstract fun reelDao(): ReelDao

    companion object {
        @Volatile
        private var INSTANCE: HuddleDatabase? = null

        fun getDatabase(context: Context): HuddleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HuddleDatabase::class.java,
                    "huddle_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
