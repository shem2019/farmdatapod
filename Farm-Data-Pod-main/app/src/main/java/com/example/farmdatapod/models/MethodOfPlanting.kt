package com.example.farmdatapod.models

data class MethodOfPlanting(
    val method: String,
    val unit: Int?,           // For methods that require units
    val labor_man_days: Int?  // For methods that require labor days
)