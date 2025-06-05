package com.example.farmdatapod

class ProcessingUser {
    // Getters and Setters
    var id: Int = 0
    var other_name: String? = null
    var last_name: String? = null
    var processor_code: String? = null
    var processing_plant: String? = null
    var id_number: String? = null
    var gender: String? = null
    var date_of_birth: String? = null
    var email: String? = null
    var phone_number: String? = null
    var education_level: String? = null
    var hub: String? = null
    var buying_center: String? = null
    var county: String? = null
    var sub_county: String? = null
    var ward: String? = null
    var village: String? = null
    var is_offline: Int = 0
    var user_id: String? = null
    // Constructors
    constructor()

    constructor(
        id: Int,
        other_name: String?,
        last_name: String?,
        processor_code: String?,
        processing_plant: String?,
        id_number: String?,
        gender: String?,
        date_of_birth: String?,
        email: String?,
        phone_number: String?,
        education_level: String?,
        hub: String?,
        buying_center: String?,
        county: String?,
        sub_county: String?,
        ward: String?,
        village: String?,
        is_offline: Int,
        user_id: String?,
    ) {
        this.id = id
        this.other_name = other_name
        this.last_name = last_name
        this.processor_code = processor_code
        this.processing_plant = processing_plant
        this.id_number = id_number
        this.gender = gender
        this.date_of_birth = date_of_birth
        this.email = email
        this.phone_number = phone_number
        this.education_level = education_level
        this.hub = hub
        this.buying_center = buying_center
        this.county = county
        this.sub_county = sub_county
        this.ward = ward
        this.village = village
        this.is_offline = is_offline
        this.user_id = user_id
    }
}