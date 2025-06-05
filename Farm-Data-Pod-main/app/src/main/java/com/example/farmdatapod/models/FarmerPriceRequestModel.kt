package com.example.farmdatapod.models

data class FarmerPriceRequestModel(
    val buying_center: String,
    val comments: String,
    val date: String,
    val hub: String,
    val online_price: String,
    val produce_id: Int,
    val sold: Boolean,
    val unit: String
)