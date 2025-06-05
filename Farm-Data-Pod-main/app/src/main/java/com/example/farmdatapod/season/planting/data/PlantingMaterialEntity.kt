package com.example.farmdatapod.season.planting.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planting_materials")
data class PlantingMaterialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planting_plan_id: Long,
    val type: String,
    val seed_batch_number: String?,
    val source: String?,
    val unit: String,
    val unit_cost: Int
)