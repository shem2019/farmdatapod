package com.example.farmdatapod.season.landPreparation.data


import androidx.room.Entity
import androidx.room.PrimaryKey

import com.google.gson.annotations.SerializedName

@Entity(tableName = "land_preparation")
data class LandPreparationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val producerId: Int,
    val seasonId: Long,
    val fieldNumber: String,
    val dateOfLandPreparation: String,
    val methodOfLandPreparation: String,
    val season_planning_id: Int,  // Changed from seasonPlanningId
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Boolean = false
)







