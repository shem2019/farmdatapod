package com.example.farmdatapod.season.cropProtection.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "crop_protection")
data class CropProtectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timeOfApplication: String,
    val weatherCondition: String,
    val producer: String,
    val season: String,
    val field: String,
    val product: String,
    val whoClassification: String,
    val formulation: String,
    val unit: String,
    val numberOfUnits: Long,
    val costPerUnit: Double,
    val dosage: String,
    val mixingRatio: String,
    val totalWater: Double,
    val laborManDays: Double,
    val unitCostOfLabor: Double,
    val comments: String?,
    val date: String,
    val season_planning_id: Long = 0L,
    val syncStatus: Boolean = false,
    val serverId: Long? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)





