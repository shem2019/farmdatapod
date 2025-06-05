package com.example.farmdatapod.season.landPreparation.data

import androidx.room.Embedded
import androidx.room.Relation


data class LandPreparationWithDetails(
    @Embedded
    val landPrep: LandPreparationEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "landPrepId"
    )
    val coverCrop: CoverCropEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "landPrepId"
    )
    val mulching: MulchingEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "landPrepId"
    )
    val soilAnalysis: SoilAnalysisEntity?
)