package com.example.farmdatapod.models

data class RouteModel(
    val final_destination: String,
    val route_number: String,
    val route_stop_points: List<RouteStopPoint>,
    val starting_point: String
)