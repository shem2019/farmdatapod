package com.example.farmdatapod.models

data class FarmerFieldRegistrationRequest(
    val producer: String? = null,
    val field_number: Int? = null,
    val field_size: String? = null,
    val crops: List<Crop>? = null
)

data class Crop(
    val crop_name: String? = null,
    val crop_variety: String? = null,
    val date_planted: String? = null,
    val date_of_harvest: String? = null,
    val population: String? = null,
    val baseline_yield: Double? = null,
    val baseline_income: String? = null,
    val baseline_cost: String? = null
)