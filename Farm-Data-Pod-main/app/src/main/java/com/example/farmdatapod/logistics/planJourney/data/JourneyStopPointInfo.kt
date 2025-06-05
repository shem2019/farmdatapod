package com.example.farmdatapod.logistics.planJourney.data

data class JourneyStopPointInfo(
    val journey_id: Long,
    val journey_name: String,
    val stop_point_id: Long?,
    val stop_point_name: String?
)