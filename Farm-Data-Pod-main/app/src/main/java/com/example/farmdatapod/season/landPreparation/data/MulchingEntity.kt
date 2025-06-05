package com.example.farmdatapod.season.landPreparation.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "mulching",
    foreignKeys = [
        ForeignKey(
            entity = LandPreparationEntity::class,
            parentColumns = ["id"],
            childColumns = ["landPrepId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MulchingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val landPrepId: Long,
    val typeOfMulch: String,
    val costOfMulch: Double,
    val lifeCycleOfMulchInSeasons: Int,
    val manDays: Int,
    val unitCostOfLabor: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Boolean = false
)