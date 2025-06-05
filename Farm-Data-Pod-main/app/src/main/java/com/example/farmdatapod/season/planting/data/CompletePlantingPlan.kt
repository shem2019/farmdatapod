package com.example.farmdatapod.season.planting.data


import androidx.room.Embedded
import androidx.room.Relation

data class CompletePlantingPlan(
    @Embedded
    val plan: PlantingPlanEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "planting_plan_id"
    )
    val material: PlantingMaterialEntity?,  // Make nullable

    @Relation(
        parentColumn = "id",
        entityColumn = "planting_plan_id"
    )
    val method: PlantingMethodEntity?  // Make nullable
)