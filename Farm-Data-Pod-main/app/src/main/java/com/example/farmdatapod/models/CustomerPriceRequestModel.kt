package com.example.farmdatapod.models

data class CustomerPriceRequestModel(
    val buying_center: String,
    val comments: String,
    val date: String,
    val hub: String,
    val online_price: String,
    val produce_id: Int,
    val sold: Boolean,
    val unit: String
)