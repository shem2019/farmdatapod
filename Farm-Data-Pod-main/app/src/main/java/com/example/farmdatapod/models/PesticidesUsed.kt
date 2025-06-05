package com.example.farmdatapod.models

data class PesticidesUsed(
    var category: String,
    var cost_per_unit: String,
    var dosage: String,
    var formulation: String,
    var frequency_of_application: String,
    var product: String,
    val register: String,
    var total_cost: String,
    var unit: String,
    var volume_of_water: String
)