package com.example.farmdatapod.produce.data

import androidx.room.TypeConverter
import com.example.farmdatapod.PrimaryProducer
import com.example.farmdatapod.produce.indipendent.biodata.ProduceItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProducerConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromProduceList(value: List<ProduceItem>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toProduceList(value: String?): List<ProduceItem>? {
        if (value == null) return null
        val type = object : TypeToken<List<ProduceItem>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromPrimaryProducer(value: List<PrimaryProducer>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPrimaryProducer(value: String?): List<PrimaryProducer>? {
        if (value == null) return null
        val type = object : TypeToken<List<PrimaryProducer>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromMapList(value: List<Map<String, Object>>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMapList(value: String?): List<Map<String, Object>>? {
        if (value == null) return null
        val type = object : TypeToken<List<Map<String, Object>>>() {}.type
        return gson.fromJson(value, type)
    }
}