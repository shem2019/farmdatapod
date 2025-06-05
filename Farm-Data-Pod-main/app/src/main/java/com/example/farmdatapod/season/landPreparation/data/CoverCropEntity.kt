package com.example.farmdatapod.season.landPreparation.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "cover_crop",
    foreignKeys = [
        ForeignKey(
            entity = LandPreparationEntity::class,
            parentColumns = ["id"],
            childColumns = ["landPrepId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CoverCropEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val landPrepId: Long,
    val coverCrop: String,
    val dateOfEstablishment: String,
    val unit: String,
    val unitCost: Double,
    val typeOfInoculant: String,
    val dateOfIncorporation: String,
    val manDays: Int,
    val unitCostOfLabor: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Boolean = false
)