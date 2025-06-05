package com.example.farmdatapod.models

data class BaitModel(
    val bait_station: String,
    val date: String,
    val `field`: String,
    val problem_identified: String,
    val producer: String,
    val season: String,
    val season_planning_id: Int
)