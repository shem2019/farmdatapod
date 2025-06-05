package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weeding_activities")
data class WeedingActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cropManagementId: Long,
    val input: String,
    val manDays: Int,
    val methodOfWeeding: String,
    val unitCostOfLabor: Double
)