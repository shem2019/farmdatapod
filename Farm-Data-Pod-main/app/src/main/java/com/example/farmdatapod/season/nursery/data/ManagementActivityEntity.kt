package com.example.farmdatapod.season.nursery.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey



@Entity(
    tableName = "management_activities",
    foreignKeys = [
        ForeignKey(
            entity = NurseryPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["nurseryPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ManagementActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nurseryPlanId: Long,
    val managementActivity: String,
    val frequency: String,
    val manDays: String,
    val unitCostOfLabor: Double
)
