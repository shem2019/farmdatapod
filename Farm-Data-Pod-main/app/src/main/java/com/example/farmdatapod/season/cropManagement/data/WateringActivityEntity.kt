package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watering_activities")
data class WateringActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cropManagementId: Long,
    val costOfFuel: Double,
    val dischargeHours: Int,
    val endTime: String,
    val frequencyOfWatering: String,
    val manDays: Int,
    val startTime: String,
    val typeOfIrrigation: String,
    val unitCost: Double,
    val unitCostOfLabor: Double
)