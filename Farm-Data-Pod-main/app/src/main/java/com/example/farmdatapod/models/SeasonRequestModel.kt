package com.example.farmdatapod.models

data class SeasonRequestModel(
    val comments: String,
    val planned_date_of_harvest: String,
    val planned_date_of_planting: String,
    val producer: String,
    val season_name: String
)