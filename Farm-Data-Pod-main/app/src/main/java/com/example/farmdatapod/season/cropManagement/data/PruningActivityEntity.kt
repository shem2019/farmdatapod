package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pruning_activities")
data class PruningActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cropManagementId: Long,
    val equipmentUsed: String,
    val manDays: Int,
    val unitCostOfLabor: Double
)