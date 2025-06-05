package com.example.farmdatapod.season.forecastYields.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "yield_forecasts")
data class YieldForecast(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int? = null,
    val seasonPlanningId: Int,
    val producer: String,
    val season: String,
    val field: String,
    val date: String,
    val currentCropPopulationPc: Int,
    val targetYield: Int,
    val yieldForecastPc: Int,
    val forecastQuality: String,
    val taComments: String,
    val syncStatus: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)