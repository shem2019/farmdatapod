package com.example.farmdatapod.models

data class NurseryManagementActivity(
    val frequency: String,
    val input: List<Input>,
    val man_days: String,
    val management_activity: String,
    val unit_cost_of_labor: Double
)