package com.example.farmdatapod.models

data class HubUserResponse(
    val buying_center: String,
    val code: String,
    val county: String,
    val date_of_birth: String,
    val education_level: String,
    val email: String,
    val gender: String,
    val hub: String,
    val id_number: Int,
    val last_name: String,
    val other_name: String,
    val phone_number: Int,
    val role: String,
    val sub_county: String,
    val village: String,
    val ward: String
)