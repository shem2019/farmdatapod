package com.example.farmdatapod.logistics.planJourney.data


import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stop_points",
    foreignKeys = [
        ForeignKey(
            entity = JourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journey_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("journey_id")
    ]
)
data class StopPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val journey_id: Long,
    val description: String,
    val purpose: String,
    val stop_point: String,
    val time: String,
    val syncStatus: Boolean = false,
    val serverId: Long? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)