package com.example.farmdatapod.season.scouting.data

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "bait_scouting")
data class BaitScoutingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bait_station: String,
    val date: String,
    val field: String,
    val problem_identified: String,
    val producer: String,
    val season: String,
    val season_planning_id: Int,
    val is_synced: Boolean = false
)