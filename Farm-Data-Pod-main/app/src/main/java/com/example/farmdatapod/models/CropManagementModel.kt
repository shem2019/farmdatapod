package com.example.farmdatapod.models


data class CropManagementModel(
    val id: String? = null,  // Server ID
    val producer: String,
    val season: String,
    val field: String,
    val date: String,
    val season_planning_id: Int,
    val comments: String,
    val gappingActivity: GappingActivity,
    val weedingActivity: WeedingActivity,
    val pruningActivity: PruningActivity,
    val stakingActivity: StakingActivity,
    val thinningActivity: ThinningActivity,
    val wateringActivity: WateringActivity
)