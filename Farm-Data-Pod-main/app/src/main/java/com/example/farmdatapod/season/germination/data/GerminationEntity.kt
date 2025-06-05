package com.example.farmdatapod.season.germination.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "germination_data")
data class GerminationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val crop: String,
    val dateOfGermination: String,  // Storing as ISO string format
    val field: String,
    val germinationPercentage: Int,
    val laborManDays: Int,
    val producer: String,
    val recommendedManagement: String,
    val season: String,
    val seasonPlanningId: Int,
    val statusOfCrop: String,
    val totalCropPopulation: String,
    val unitCostOfLabor: Double,
    val isSynced: Boolean = false,  // Track sync status
    val createdAt: Long = System.currentTimeMillis()  // For tracking when the entry was created
)