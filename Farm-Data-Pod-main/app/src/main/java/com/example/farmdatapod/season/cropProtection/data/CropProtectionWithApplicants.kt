package com.example.farmdatapod.season.cropProtection.data

import androidx.room.Embedded
import androidx.room.Relation


data class CropProtectionWithApplicants(
    @Embedded
    val cropProtection: CropProtectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "crop_protection_id"
    )
    val applicants: List<CropProtectionApplicantEntity>
)