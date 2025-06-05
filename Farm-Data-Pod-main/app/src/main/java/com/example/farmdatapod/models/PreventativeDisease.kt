package com.example.farmdatapod.models

data class PreventativeDisease(
    var category: String,
    var cost_per_unit: String,
    val disease: String,
    var dosage: String,
    var formulation: String,
    var frequency_of_application: String,
    var product: String,
    var total_cost: String,
    var unit: String,
    var volume_of_water: String
)