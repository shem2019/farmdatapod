package com.example.farmdatapod

class IndividualCustomer {
    // Getters and Setters
    var id: Int = 0
    var other_name: String? = null
    var last_name: String? = null
    var customer_code: String? = null
    var id_number: String? = null
    var gender: String? = null
    var date_of_birth: String? = null
    var email: String? = null
    var phone_number: String? = null
    var county: String? = null
    var sub_county: String? = null
    var ward: String? = null
    var village: String? = null
    var user_authorised: Boolean = false
    var authorisation_token: String? = null
    var is_offline: Int = 0
    var user_id: String? = null
    var products: List<Product>? = null

    // Constructors
    constructor()

    constructor(
        id: Int, other_name: String?, last_name: String?, customer_code: String?, id_number: String?,
        gender: String?, date_of_birth: String?, email: String?, phone_number: String?,
        county: String?, sub_county: String?, ward: String?, village: String?,
        user_authorised: Boolean, authorisation_token: String?, is_offline: Int, user_id: String?
    ) {
        this.id = id
        this.other_name = other_name
        this.last_name = last_name
        this.customer_code = customer_code
        this.id_number = id_number
        this.gender = gender
        this.date_of_birth = date_of_birth
        this.email = email
        this.phone_number = phone_number
        this.county = county
        this.sub_county = sub_county
        this.ward = ward
        this.village = village
        this.user_authorised = user_authorised
        this.authorisation_token = authorisation_token
        this.is_offline = is_offline
        this.user_id = user_id
    }
}