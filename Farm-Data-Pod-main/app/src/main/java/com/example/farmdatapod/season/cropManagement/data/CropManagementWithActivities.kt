package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Embedded
import androidx.room.Relation

data class CropManagementWithActivities(
    @Embedded val cropManagement: CropManagementEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "cropManagementId"
    )
    val gappingActivities: List<GappingActivityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "cropManagementId"
    )
    val weedingActivities: List<WeedingActivityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "cropManagementId"
    )
    val pruningActivities: List<PruningActivityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "cropManagementId"
    )
    val stakingActivities: List<StakingActivityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "cropManagementId"
    )
    val thinningActivities: List<ThinningActivityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "cropManagementId"
    )
    val wateringActivities: List<WateringActivityEntity>
)
