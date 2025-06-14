package com.example.farmdatapod.models

import com.google.gson.annotations.SerializedName


data class FarmerFieldRegistrationRequest(
    @SerializedName("producer")
    val producer: String,

    @SerializedName("field_number")
    val field_number: Int,

    @SerializedName("field_size")
    val field_size: Float,

    @SerializedName("crops")
    val crops: List<Crop>
)