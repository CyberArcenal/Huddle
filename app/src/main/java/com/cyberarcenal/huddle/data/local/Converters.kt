package com.cyberarcenal.huddle.data.local

import androidx.room.TypeConverter
import com.cyberarcenal.huddle.api.models.ReelDisplay
import com.cyberarcenal.huddle.api.models.StoryFeed
import com.cyberarcenal.huddle.api.models.StoryHighlight
import com.cyberarcenal.huddle.api.models.UnifiedContentItem
import com.cyberarcenal.huddle.api.models.UserProfile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(OffsetDateTime::class.java, JsonSerializer<OffsetDateTime> { src, _, _ ->
            JsonPrimitive(src.format(formatter))
        })
        .registerTypeAdapter(OffsetDateTime::class.java, JsonDeserializer { json, _, _ ->
            try {
                OffsetDateTime.parse(json.asString, formatter)
            } catch (e: Exception) {
                null
            }
        })
        .create()

    @TypeConverter
    fun fromTimestamp(value: String?): OffsetDateTime? {
        return value?.let {
            try {
                OffsetDateTime.parse(it, formatter)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: OffsetDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun fromUserProfile(value: UserProfile?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toUserProfile(value: String?): UserProfile? = value?.let { gson.fromJson(it, UserProfile::class.java) }

    @TypeConverter
    fun fromUnifiedContentItem(value: UnifiedContentItem?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toUnifiedContentItem(value: String?): UnifiedContentItem? = value?.let { gson.fromJson(it, UnifiedContentItem::class.java) }

    @TypeConverter
    fun fromStoryFeed(value: StoryFeed?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStoryFeed(value: String?): StoryFeed? = value?.let { gson.fromJson(it, StoryFeed::class.java) }

    @TypeConverter
    fun fromStoryHighlight(value: StoryHighlight?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStoryHighlight(value: String?): StoryHighlight? = value?.let { gson.fromJson(it, StoryHighlight::class.java) }

    @TypeConverter
    fun fromReelDisplay(value: ReelDisplay?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toReelDisplay(value: String?): ReelDisplay? = value?.let { gson.fromJson(it, ReelDisplay::class.java) }
}
