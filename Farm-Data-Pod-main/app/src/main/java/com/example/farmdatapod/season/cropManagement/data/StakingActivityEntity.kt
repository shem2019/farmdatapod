package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staking_activities")
data class StakingActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cropManagementId: Long,
    val costPerUnit: Double,
    val manDays: Int,
    val unitCostOfLabor: Double,
    val unitStakes: Int
)