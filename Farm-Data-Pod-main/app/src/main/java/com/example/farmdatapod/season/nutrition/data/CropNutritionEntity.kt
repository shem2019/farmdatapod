package com.example.farmdatapod.season.nutrition.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crop_nutrition")
data class CropNutritionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timeOfApplication: String? = null,
    val weatherCondition: String? = null,
    val producer: String? = null,
    val season: String? = null,
    val field: String? = null,
    val product: String? = null,
    val category: String? = null,
    val formulation: String? = null,
    val unit: String? = null,
    val numberOfUnits: Long? = null,
    val costPerUnit: Double? = null,
    val dosage: String? = null,
    val mixingRatio: String? = null,
    val totalWater: Double? = null,
    val laborManDays: Double? = null,
    val unitCostOfLabor: Double? = null,
    val comments: String? = null,
    val date: String? = null,
    val season_planning_id: Long = 0L,
    val syncStatus: Boolean = false,
    val serverId: Long? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)