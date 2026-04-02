package com.cyberarcenal.huddle.data.local

import androidx.room.TypeConverter
import com.cyberarcenal.huddle.api.models.UnifiedContentItem
import com.cyberarcenal.huddle.api.models.UserProfile
import com.google.gson.Gson
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val gson = Gson()
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): OffsetDateTime? {
        return value?.let {
            return formatter.parse(it, OffsetDateTime::from)
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: OffsetDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun fromUserProfile(value: UserProfile?): String? = gson.toJson(value)

    @TypeConverter
    fun toUserProfile(value: String?): UserProfile? = gson.fromJson(value, UserProfile::class.java)

    @TypeConverter
    fun fromUnifiedContentItem(value: UnifiedContentItem?): String? = gson.toJson(value)

    @TypeConverter
    fun toUnifiedContentItem(value: String?): UnifiedContentItem? = gson.fromJson(value, UnifiedContentItem::class.java)
}
