package com.example.farmdatapod.models

data class ForecastYield(
    var crop_population_pc: String,
    var forecast_quality: String,
    var ta_comments: String,
    var yield_forecast_pc: String
)