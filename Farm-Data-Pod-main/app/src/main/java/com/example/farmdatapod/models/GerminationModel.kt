package com.example.farmdatapod.models

data class GerminationModel(
    val crop: String,
    val date_of_germination: String,
    val field: String,
    val germination_percentage: Int,
    val labor_man_days: Int,
    val producer: String,
    val recommended_management: String,
    val season: String,
    val season_planning_id: Int,
    val status_of_crop: String,
    val total_crop_population: String,
    val unit_cost_of_labor: Double
)