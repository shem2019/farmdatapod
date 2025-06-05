package com.example.farmdatapod.models

data class PlanPlantingModel(
    val crop: String? = null,
    val crop_cycle_in_weeks: Int? = null,
    val crop_population: Int? = null,
    val date_of_planting: String? = null,
    val `field`: String? = null,
    val labor_man_days: Int? = null,
    val method_of_planting: MethodOfPlanting? = null,
    val planting_material: PlantingMaterial? = null,
    val producer: String? = null,
    val season: String? = null,
    val season_planning_id: Int? = null,
    val target_population: Int? = null,
    val unit_cost_of_labor: Double? = null
)