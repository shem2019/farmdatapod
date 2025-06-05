package com.example.farmdatapod.logistics.createRoute.data

import androidx.room.TypeConverter
import com.example.farmdatapod.models.RouteStopPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StopPointConverter {
    // Convert List<StopPoint> to String when saving to database
    @TypeConverter
    fun fromStopPoints(stopPoints: List<RouteStopPoint>): String {
        // Convert the list to a JSON string
        return Gson().toJson(stopPoints)
        // Example output: [{"stopName":"Stop 1","orderNumber":1},{"stopName":"Stop 2","orderNumber":2}]
    }

    // Convert String back to List<StopPoint> when reading from database
    @TypeConverter
    fun toStopPoints(stopPointsString: String): List<RouteStopPoint> {
        // Create a type that represents List<StopPoint>
        val listType = object : TypeToken<List<RouteStopPoint>>() {}.type
        // Convert JSON string back to List<StopPoint>
        return Gson().fromJson(stopPointsString, listType)
    }
}