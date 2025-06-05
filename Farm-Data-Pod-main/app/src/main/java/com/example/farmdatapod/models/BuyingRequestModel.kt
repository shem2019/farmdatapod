package com.example.farmdatapod.models

data class BuyingRequestModel(
    val action: String,
    var buying_center: String,
    var grn_number: String,
    val loaded: Boolean,
    var produce: String,
    var producer: String,
    val quality: Quality,
    var unit: String,
    val weight: String
)