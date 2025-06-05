package com.example.farmdatapod.season.nutrition.data

import androidx.room.Embedded
import androidx.room.Relation

data class CropNutritionWithApplicants(
    @Embedded val cropNutrition: CropNutritionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "cropNutritionId"
    )
    val applicants: List<ApplicantEntity>
)