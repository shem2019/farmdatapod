package com.example.farmdatapod.models

data class SoilAnalysis(
    var date_range: String = "",
    var man_days: String = "",
    var type_of_analysis: String = "",
    var unit_cost: String = "",
    var week_number: String = ""
)