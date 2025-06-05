package com.example.farmdatapod.season.register.registerSeasonData

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "seasons",
    // Add index on season_name and producer to improve duplicate checking performance
    indices = [Index(value = ["season_name", "producer"])]
)
data class Season(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val producer: String,
    val season_name: String,
    val planned_date_of_planting: String,
    val planned_date_of_harvest: String,
    val comments: String,
    val syncStatus: Boolean = false,
    val serverId: Int? = null,
    // New fields for sync and timestamp tracking
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)