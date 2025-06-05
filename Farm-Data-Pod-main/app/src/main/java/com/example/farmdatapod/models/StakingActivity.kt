package com.example.farmdatapod.models

data class StakingActivity(
    val activity: String,
    val cost_per_unit: Double,
    val man_days: Int,
    val unit_cost_of_labor: Double,
    val unit_stakes: Int
)