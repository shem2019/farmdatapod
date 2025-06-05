package com.example.farmdatapod.season.nutrition.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "applicants",
    foreignKeys = [
        ForeignKey(
            entity = CropNutritionEntity::class,
            parentColumns = ["id"],
            childColumns = ["cropNutritionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cropNutritionId")]
)
data class ApplicantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cropNutritionId: Long,
    val name: String,
    val ppesUsed: String,
    val equipmentUsed: String
)