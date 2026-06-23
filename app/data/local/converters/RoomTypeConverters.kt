package com.vica.app.data.local.converters

import androidx.room.TypeConverter
import java.util.Date
import java.util.UUID

class RoomTypeConverters {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(uuidStr: String?): UUID? = uuidStr?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
