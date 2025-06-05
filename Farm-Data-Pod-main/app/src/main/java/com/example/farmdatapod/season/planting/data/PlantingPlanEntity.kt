package com.example.farmdatapod.season.planting.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "planting_plans")
data class PlantingPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val producer: String? = null,
    val field: String? = null,
    val season: String? = null,
    val date_of_planting: String? = null,
    val crop: String? = null,
    val crop_population: Int? = null,
    val target_population: Int? = null,
    val crop_cycle_in_weeks: Int? = null,
    val labor_man_days: Int? = null,
    val unit_cost_of_labor: Double? = null,
    val season_planning_id: Int? = null,
    val sync_status: Boolean = false, // false = needs sync, true = synced
    val created_at: Long = System.currentTimeMillis()
)