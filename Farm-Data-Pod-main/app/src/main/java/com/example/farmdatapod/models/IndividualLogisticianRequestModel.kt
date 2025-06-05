package com.example.farmdatapod.models

data class IndividualLogisticianRequestModel(
    var address: String,
    var cars: List<Car>,
    var date_of_birth: String,
    var email: String,
    var hub: String,
    var id_number: Int,
    var last_name: String,
    var logistician_code: String,
    var other_name: String,
    var phone_number: Int,
    var region: String
)