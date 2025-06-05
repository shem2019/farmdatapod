package com.example.farmdatapod

class OrganisationalLogistician {
    // Getters and Setters
    var id: Int = 0
    var name: String? = null
    var logistician_code: String? = null
    var registration_number: String? = null
    var date_of_registration: String? = null
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
        id: Int, name: String?, logistician_code: String?, registration_number: String?,
        date_of_registration: String?, email: String?, phone_number: String?, address: String?,
        hub: String?, region: String?, is_offline: Int, user_id: String?
    ) {
        this.id = id
        this.name = name
        this.logistician_code = logistician_code
        this.registration_number = registration_number
        this.date_of_registration = date_of_registration
        this.email = email
        this.phone_number = phone_number
        this.address = address
        this.hub = hub
        this.region = region
        this.is_offline = is_offline
        this.user_id = user_id
    }
}