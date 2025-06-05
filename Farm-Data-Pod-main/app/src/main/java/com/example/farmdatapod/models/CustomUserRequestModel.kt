package com.example.farmdatapod.models

data class CustomUserRequestModel(
    val date_of_birth: String,
    val education_level: String,
    val email: String,
    val gender: String,
    val id_number: Int,
    val last_name: String,
    val other_name: String,
    val phone_number: Int,
    val reporting_to: String,
    val role: String,
    val staff_code: String
)