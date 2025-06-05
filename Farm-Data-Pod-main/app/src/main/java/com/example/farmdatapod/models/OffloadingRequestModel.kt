package com.example.farmdatapod.models

data class OffloadingRequestModel(
    val comment: String,
    val grn: String,
    val offloaded_load: Int,
    val total_weight: String,
    val truck_offloading_number: String
)