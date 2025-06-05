package com.example.farmdatapod.models

data class WeedingActivity(
    val activity: String,
    val input: String,
    val man_days: Int,
    val method_of_weeding: String,
    val unit_cost_of_labor: Double
)