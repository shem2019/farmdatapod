package com.example.farmdatapod.models

data class RuralWorkerRequest(
    val county: String,
    val date_of_birth: String,
    val education_level: String,
    val email: String,
    val gender: String,
    val id_number: Int,
    val last_name: String,
    val other: String,
    val other_name: String,
    val phone_number: Int,
    val rural_worker_code: String,
    val service: String,
    val sub_county: String,
    val village: String,
    val ward: String
)