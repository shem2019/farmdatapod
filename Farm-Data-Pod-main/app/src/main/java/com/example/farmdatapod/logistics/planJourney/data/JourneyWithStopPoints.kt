package com.example.farmdatapod.logistics.planJourney.data

// JourneyWithStopPoints.kt

import androidx.room.Embedded
import androidx.room.Relation

data class JourneyWithStopPoints(
    @Embedded
    val journey: JourneyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "journey_id"
    )
    val stopPoints: List<StopPointEntity>
)