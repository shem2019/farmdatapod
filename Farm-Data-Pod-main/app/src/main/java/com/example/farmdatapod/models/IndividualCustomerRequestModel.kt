package com.example.farmdatapod.models

data class IndividualCustomerRequestModel(
    val county: String,
    val customer_code: String,
    val date_of_birth: String,
    val email: String,
    val gender: String,
    val id_number: Int,
    val last_name: String,
    val other_name: String,
    val phone_number: Int,
    val products: List<Product>,
    val sub_county: String,
    val village: String,
    val ward: String
)