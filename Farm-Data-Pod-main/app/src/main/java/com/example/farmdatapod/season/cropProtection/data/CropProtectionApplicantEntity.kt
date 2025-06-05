package com.example.farmdatapod.season.cropProtection.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "crop_protection_applicants",
    foreignKeys = [
        ForeignKey(
            entity = CropProtectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["crop_protection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("crop_protection_id")
    ]
)
data class CropProtectionApplicantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val crop_protection_id: Long,
    val name: String,
    val ppes_used: String,
    val equipment_used: String,
    val syncStatus: Boolean = false,
    val serverId: Long? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)