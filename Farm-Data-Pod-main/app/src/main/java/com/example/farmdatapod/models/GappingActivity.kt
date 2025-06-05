package com.example.farmdatapod.models

data class GappingActivity(
    val activity: String,
    val crop_population: Int,
    val man_days: Int,
    val planting_material: String,
    val target_population: Int,
    val unit_cost_of_labor: Double
)