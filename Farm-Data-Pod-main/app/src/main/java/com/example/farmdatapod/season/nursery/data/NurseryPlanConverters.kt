package com.example.farmdatapod.season.nursery.data

import androidx.room.TypeConverter
import java.util.Date



class NurseryPlanConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}