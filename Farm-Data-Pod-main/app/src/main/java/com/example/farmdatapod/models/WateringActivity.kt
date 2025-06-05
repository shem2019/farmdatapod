package com.example.farmdatapod.models

data class WateringActivity(
    val activity: String,
    val cost_of_fuel: Double,
    val discharge_hours: Int,
    val end_time: String,
    val frequency_of_watering: String,
    val man_days: Int,
    val start_time: String,
    val type_of_irrigation: String,
    val unit_cost: Double,
    val unit_cost_of_labor: Double
)