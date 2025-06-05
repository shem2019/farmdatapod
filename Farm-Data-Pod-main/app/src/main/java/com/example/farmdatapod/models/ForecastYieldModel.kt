package com.example.farmdatapod.models

import com.google.gson.annotations.SerializedName

data class ForecastYieldModel(
    @SerializedName("current_crop_population_pc")
    val current_crop_population_pc: Int,

    @SerializedName("date")
    val date: String, // Should be in format "yyyy-MM-dd'T'HH:mm:ss"

    @SerializedName("field")
    val field: String, // Keep as String since API expects "Field 12" format

    @SerializedName("forecast_quality")
    val forecast_quality: String, // Should be like "High", not numeric

    @SerializedName("producer")
    val producer: String, // Should be like "Producer A"

    @SerializedName("season")
    val season: String,

    @SerializedName("season_planning_id")
    val season_planning_id: Int,

    @SerializedName("ta_comments")
    val ta_comments: String,

    @SerializedName("target_yield")
    val target_yield: Int,

    @SerializedName("yield_forecast_pc")
    val yield_forecast_pc: Int
)