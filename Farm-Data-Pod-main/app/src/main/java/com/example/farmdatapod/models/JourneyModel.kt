package com.example.farmdatapod.models

data class JourneyModel(
    val id: Long = 0,  // Add this with default value
    val date_and_time: String,
    val driver: String,
    val logistician_status: String,
    val route_id: Int,
    val truck: String,
    val user_id: String,  // Add this with default value
    val stop_points: List<StopPoint>
)