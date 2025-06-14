package com.example.farmdatapod.dbmodels

import com.google.gson.annotations.SerializedName

/**
 * This data class MUST match the JSON response from your server after a successful POST.
 * It represents a single field registration returned by the API.
 */
data class FarmerFieldRegistrationResponse(
    @SerializedName("id") val id: Int, // The server-generated ID for the registration
    @SerializedName("producer") val producer: String,
    @SerializedName("field_number") val field_number: Int,
    @SerializedName("field_size") val field_size: Float,
    @SerializedName("crops") val crops: List<CropResponse>
)

/**
 * Represents a single crop within the FarmerFieldRegistrationResponse.
 */
data class CropResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("crop_name") val crop_name: String,
    @SerializedName("crop_variety") val crop_variety: String,
    @SerializedName("date_planted") val date_planted: String,
    @SerializedName("date_of_harvest") val date_of_harvest: String,
    @SerializedName("population") val population: Int,
    @SerializedName("baseline_yield_last_season") val baseline_yield_last_season: Double,
    @SerializedName("baseline_income_last_season") val baseline_income_last_season: Double,
    @SerializedName("baseline_cost_of_production_last_season") val baseline_cost_of_production_last_season: Double,
    @SerializedName("sold") val sold: Boolean
)