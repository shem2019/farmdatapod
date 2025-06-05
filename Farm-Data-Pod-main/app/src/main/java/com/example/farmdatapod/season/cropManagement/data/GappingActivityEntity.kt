package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gapping_activities")
data class GappingActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cropManagementId: Long,
    val cropPopulation: Int,
    val manDays: Int,
    val plantingMaterial: String,
    val targetPopulation: Int,
    val unitCostOfLabor: Double
)