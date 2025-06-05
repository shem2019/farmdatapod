package com.example.farmdatapod.season.landPreparation.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "soil_analysis",
    foreignKeys = [
        ForeignKey(
            entity = LandPreparationEntity::class,
            parentColumns = ["id"],
            childColumns = ["landPrepId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SoilAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val landPrepId: Long,
    val typeOfAnalysis: String,
    val costOfAnalysis: Double,
    val lab: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Boolean = false
)