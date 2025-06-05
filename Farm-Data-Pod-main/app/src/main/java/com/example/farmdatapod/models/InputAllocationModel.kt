package com.example.farmdatapod.models

data class InputAllocationModel(
    val description: String,
    val dn_number: String,
    val input: String,
    val journey_id: Int,
    val number_of_units: Int,
    val stop_point_id: Int,
    val unit_cost: Int
)