package com.example.farmdatapod.models


data class BuyingCentreRequest(
    val address: String,
    val buying_center_code: String,
    val buying_center_name: String,
    val county: String,
    val facilities: String,
    val floor_size: String,
    val hub: String,
    val input_center: String,
    val key_contacts: List<KeyContact>,  // Changed from List<Unit> to List<KeyContact>
    val location: String,
    val ownership: String,
    val sub_county: String,
    val type_of_building: String,
    val village: String,
    val ward: String,
    val year_established: String
)