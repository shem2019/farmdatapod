package com.example.farmdatapod.models


data class SeasonResponse(
    val id: Int,
    val producer: String,
    val season_name: String,
    val planned_date_of_planting: String,
    val planned_date_of_harvest: String,
    val comments: String,
    val user_id: String?
)