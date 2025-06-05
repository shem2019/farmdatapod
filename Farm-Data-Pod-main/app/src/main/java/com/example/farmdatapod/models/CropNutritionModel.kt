package com.example.farmdatapod.models

data class CropNutritionModel(
    val id: Long = 0L,
    val category: String? = null,
    val comments: String? = null,
    val cost_per_unit: Double? = null,
    val date: String? = null,
    val dosage: String? = null,
    val field: String? = null,
    val formulation: String? = null,
    val labor_man_days: Long? = null,
    val mixing_ratio: String? = null,
    val name_of_applicants: NameOfApplicants? = null,
    val number_of_units: Long? = null,
    val producer: String? = null,
    val product: String? = null,
    val season: String? = null,
    val season_planning_id: Long? = null,
    val time_of_application: String? = null,
    val total_amount_of_water: Long? = null,
    val unit: String? = null,
    val unit_cost_of_labor: Long? = null,
    val weather_condition: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)