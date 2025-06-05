package com.example.farmdatapod.logistics.equipments.models

data class EquipmentModel(
    val description: String,
    val dn_number: String,
    val equipment: String,
    val journey_id: Int,
    val number_of_units: Int,
    val stop_point_id: Int,
    val unit_cost: Int
)