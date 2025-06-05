package com.example.farmdatapod.models

data class HQUsersRequest(
    val date_of_birth: String,
    val department: String,
    val education_level: String,
    val email: String,
    val gender: String,
    val id_number: Int,
    val last_name: String,
    val other_name: String,
    val phone_number: Int,
    val related_roles: String,
    val reporting_to: String,
    val role: String,
    val staff_code: String
)