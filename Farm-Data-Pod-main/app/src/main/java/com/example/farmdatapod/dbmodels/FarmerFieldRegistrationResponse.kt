//package com.example.farmdatapod.dbmodels
//
//
//data class FarmerFieldRegistrationResponse(
//    val id: Int,
//    val producer: String,
//    val field_number: Int,
//    val field_size: String,
//    val crops: List<CropResponse>
//)
//
//data class CropResponse(
//    val id: Int,
//    val crop_name: String,
//    val crop_variety: String,
//    val date_planted: String,
//    val date_of_harvest: String,
//    val population: String,
//    val baseline_yield: Double,
//    val baseline_income: String,
//    val baseline_cost: String
//)

package com.example.farmdatapod.dbmodels

data class FarmerFieldRegistrationResponse(
    val id: Int? = null,  // Made nullable with default value
    val producer: String,
    val field_number: Int,
    val field_size: String,
    val crops: List<CropResponse>
)

data class CropResponse(
    val id: Int? = null,  // Made nullable with default value
    val crop_name: String,
    val crop_variety: String,
    val date_planted: String,
    val date_of_harvest: String,
    val population: String,
    val baseline_yield: Double,
    val baseline_income: String,
    val baseline_cost: String
)