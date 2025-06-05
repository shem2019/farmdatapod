package com.example.farmdatapod.models

data class PruningActivity(
    val activity: String,
    val equipment_used: String,
    val man_days: Int,
    val unit_cost_of_labor: Double
)