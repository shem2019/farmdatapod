package com.example.farmdatapod.season.harvest.data

import androidx.room.Embedded
import androidx.room.Relation

data class HarvestPlanningWithBuyers(
    @Embedded
    val harvestPlanning: HarvestPlanning,
    @Relation(
        parentColumn = "id",
        entityColumn = "harvest_planning_id"
    )
    val buyers: List<HarvestPlanningBuyer>
)