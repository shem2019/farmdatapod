package com.example.farmdatapod.logistics.planJourney.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journeys",
    indices = [
        Index("server_id", unique = true)  // Use server's ID instead of route_id
    ]
)
data class JourneyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val server_id: Long,  // Add this to store the server's ID
    val date_and_time: String,
    val driver: String,
    val logistician_status: String,
    val route_id: Int,
    val user_id: String,
    val truck: String,
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)


