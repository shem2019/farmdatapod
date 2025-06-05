package com.example.farmdatapod

class Car {
    // Getters and Setters
    var id: Int = 0
    var car_body_type: String? = null
    var car_model: String? = null
    var number_plate: String? = null
    var driver1_name: String? = null
    var driver2_name: String? = null
    var is_offline: Int = 0
    var individual_logistician_id: Int = 0
    var organisation_logistician_id: Int = 0

    // Constructors
    constructor()

    constructor(
        id: Int,
        car_body_type: String?,
        car_model: String?,
        number_plate: String?,
        driver1_name: String?,
        driver2_name: String?,
        is_offline: Int,
        individual_logistician_id: Int,
        organisation_logistician_id: Int,
    ) {
        this.id = id
        this.car_body_type = car_body_type
        this.car_model = car_model
        this.number_plate = number_plate
        this.driver1_name = driver1_name
        this.driver2_name = driver2_name
        this.is_offline = is_offline
        this.individual_logistician_id = individual_logistician_id
        this.organisation_logistician_id = organisation_logistician_id
    }
}