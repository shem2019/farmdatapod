package com.example.farmdatapod.models

data class SellingRequestModel(
    val action: String,
    val customer: String,
    val grn_number: String,
    val loaded: Boolean,
    val online_price: String,
    val produce: String,
    val quality: Quality,
    val unit: String,
    val weight: String
)