package com.example.farmdatapod.models

data class PlanNutrition(
    var product: String = "",
    var product_name: String = "",
    var unit: String = "",
    var application_rate: String = "",
    var cost_per_unit: String = "",
    var time_of_application: String = "",
    var method_of_application: String = "",
    var product_formulation: String = "",
    var date_of_application: String = "",
    var total_mixing_ratio: String = ""
)