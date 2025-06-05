package com.example.farmdatapod.logistics.createRoute.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.farmdatapod.models.RouteStopPoint
import java.util.*

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val routeNumber: String,
    val startingPoint: String,
    val finalDestination: String,

    @TypeConverters(StopPointConverter::class)
    val stopPoints: List<RouteStopPoint>,

    val syncStatus: String = "PENDING",
    val syncMessage: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,

    val isDeleted: Boolean = false
)



