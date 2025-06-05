package com.example.farmdatapod.season.nursery.data

import androidx.room.Embedded
import androidx.room.Relation

data class NurseryPlanWithRelations(
    @Embedded val nurseryPlan: NurseryPlanEntity,
    @Relation(
        entity = ManagementActivityEntity::class,
        parentColumn = "id",
        entityColumn = "nurseryPlanId"
    )
    val managementActivities: List<ManagementActivityWithInputs>
)