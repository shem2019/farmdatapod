package com.example.farmdatapod.models

data class OrganisationLogisticianRequest(
    val address: String,
    val cars: List<Car>,
    val date_of_registration: String,
    val email: String,
    val hub: String,
    val logistician_code: String,
    val name: String,
    val phone_number: Int,
    val region: String,
    val registration_number: Int
)