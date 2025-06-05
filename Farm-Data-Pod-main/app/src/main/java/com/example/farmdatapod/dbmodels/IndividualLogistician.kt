package com.example.farmdatapod

class IndividualLogistician {
    // Getters and Setters
    var id: Int = 0
    var other_name: String? = null
    var last_name: String? = null
    var logistician_code: String? = null
    var id_number: String? = null
    var date_of_birth: String? = null
    var email: String? = null
    var phone_number: String? = null
    var address: String? = null
    var hub: String? = null
    var region: String? = null
    var is_offline: Int = 0
    var user_id: String? = null
    var cars: List<Car>? = null

    // Constructors
    constructor()

    constructor(
        id: Int, other_name: String?, last_name: String?, logistician_code: String?,
        id_number: String?, date_of_birth: String?, email: String?, phone_number: String?,
        address: String?, hub: String?, region: String?, is_offline: Int, user_id: String,
    ) {
        this.id = id
        this.other_name = other_name
        this.last_name = last_name
        this.logistician_code = logistician_code
        this.id_number = id_number
        this.date_of_birth = date_of_birth
        this.email = email
        this.phone_number = phone_number
        this.address = address
        this.hub = hub
        this.region = region
        this.is_offline = is_offline
        this.user_id = user_id
    }
}