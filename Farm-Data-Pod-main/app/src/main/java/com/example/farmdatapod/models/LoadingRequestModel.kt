package com.example.farmdatapod.models

data class LoadingRequestModel(
    val comment: String,
    val from_: String,
    val grn: String,
    val to: String,
    val total_weight: String,
    val truck_loading_number: String
)