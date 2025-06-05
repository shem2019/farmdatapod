package com.example.farmdatapod.season.nursery.data

import androidx.room.Embedded
import androidx.room.Relation

data class ManagementActivityWithInputs(
    @Embedded val activity: ManagementActivityEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "managementActivityId"
    )
    val inputs: List<InputEntity>
)