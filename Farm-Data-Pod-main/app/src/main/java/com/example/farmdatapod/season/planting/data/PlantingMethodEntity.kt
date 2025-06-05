package com.example.farmdatapod.season.planting.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planting_methods")
data class PlantingMethodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planting_plan_id: Long,
    val method: String,
    val unit: Int?,
    val labor_man_days: Int?
)