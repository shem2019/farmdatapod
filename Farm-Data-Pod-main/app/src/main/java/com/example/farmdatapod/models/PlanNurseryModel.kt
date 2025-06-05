package com.example.farmdatapod.models

data class PlanNurseryModel(
    val comments: String,
    val crop: String,
    val crop_cycle_weeks: Int,
    val date_of_establishment: String,
    val number_of_trays: Int,
    val nursery_management_activity: List<NurseryManagementActivity>,
    val producer: String,
    val season: String,
    val season_planning_id: Int,
    val seed_batch_number: String,
    val type_of_trays: String,
    val variety: String
)