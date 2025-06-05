package com.example.farmdatapod.models

data class Harvesting(
    var date_range: String = "",
    var man_days: Int = 0,
    var plant_population: String = "",
    var projected_yield: String = "",
    var unit_cost: String = "",
    var week_number: String = ""
)