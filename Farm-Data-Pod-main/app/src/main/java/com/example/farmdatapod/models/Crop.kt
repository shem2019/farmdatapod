package com.example.farmdatapod.models

import com.google.gson.annotations.SerializedName

data class Crop(
    @SerializedName("crop_name")
    val crop_name: String,

    @SerializedName("crop_variety")
    val crop_variety: String,

    @SerializedName("date_planted")
    val date_planted: String,

    @SerializedName("date_of_harvest")
    val date_of_harvest: String,

    @SerializedName("population")
    val population: Int,

    @SerializedName("baseline_yield_last_season")
    val baseline_yield_last_season: Double,

    @SerializedName("baseline_income_last_season")
    val baseline_income_last_season: Double,

    @SerializedName("baseline_cost_of_production_last_season")
    val baseline_cost_of_production_last_season: Double,

    @SerializedName("sold")
    val sold: Boolean
)