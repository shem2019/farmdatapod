package com.example.farmdatapod.models


data class CropProtectionModel(
    val id: Long = 0L,
    val comments: String,
    val cost_per_unit: Double,
    val date: String,
    val dosage: String,
    val field: String,
    val formulation: String,
    val labor_man_days: Int,
    val mixing_ratio: String,
    val name_of_applicants: NameOfApplicants,
    val number_of_units: Int,
    val producer: String,
    val product: String,
    val season: String,
    val season_planning_id: Int,
    val time_of_application: String,
    val total_amount_of_water: Int,
    val unit: String,
    val unit_cost_of_labor: Int,
    val weather_condition: String,
    val who_classification: String,
    val lastModified: Long = System.currentTimeMillis()
)