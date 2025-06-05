package com.example.farmdatapod.models

data class HarvestPlanningModel(
    val buyers: List<Buyer>,
    val comments: String,
    val date: String,
    val end_time: String,
    val `field`: String,
    val harvested_quality: String,
    val harvested_units: Int,
    val labor_man_days: Int,
    val number_of_units: Int,
    val price_per_unit: Double,
    val producer: String,
    val season: String,
    val season_planning_id: Int,
    val start_time: String,
    val unit_cost_of_labor: Double,
    val weight_per_unit: Double
)