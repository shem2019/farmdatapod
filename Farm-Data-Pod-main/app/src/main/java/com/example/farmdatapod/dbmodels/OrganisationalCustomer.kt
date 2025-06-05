package com.example.farmdatapod

class OrganisationalCustomer {
    // Getters and Setters
    var id: Int = 0
    var company_name: String? = null
    var customer_code: String? = null
    var registration_number: Int = 0
    var sector: String? = null
    var date_of_registration: String? = null
    var email: String? = null
    var phone_number: String? = null
    var county: String? = null
    var sub_county: String? = null
    var ward: String? = null
    var village: String? = null
    var other_name1: String? = null
    var last_name1: String? = null
    var id_number1: Int = 0
    var gender1: String? = null
    var date_of_birth1: String? = null
    var email1: String? = null
    var phone_number1: String? = null
    var other_name2: String? = null
    var last_name2: String? = null
    var id_number2: Int = 0
    var gender2: String? = null
    var date_of_birth2: String? = null
    var email2: String? = null
    var phone_number2: String? = null
    var other_name3: String? = null
    var last_name3: String? = null
    var id_number3: Int = 0
    var gender3: String? = null
    var date_of_birth3: String? = null
    var email3: String? = null
    var phone_number3: String? = null
    var user_id: String? = null
    var user_authorised: Boolean = false
    var authorisation_token: String? = null
    var products: List<Product>? = null

    // Constructors
    constructor()

    constructor(
        id: Int, company_name: String?, customer_code: String?, registration_number: Int,
        sector: String?, date_of_registration: String?, email: String?, phone_number: String,
        county: String?, sub_county: String?, ward: String?, village: String?,
        other_name1: String?, last_name1: String?, id_number1: Int, gender1: String?,
        date_of_birth1: String?, email1: String?, phone_number1: String,
        other_name2: String?, last_name2: String?, id_number2: Int, gender2: String?,
        date_of_birth2: String?, email2: String?, phone_number2: String,
        other_name3: String?, last_name3: String?, id_number3: Int, gender3: String?,
        date_of_birth3: String?, email3: String?, phone_number3: String,
        user_id: String?, user_authorised: Boolean, authorisation_token: String?
    ) {
        this.id = id
        this.company_name = company_name
        this.customer_code = customer_code
        this.registration_number = registration_number
        this.sector = sector
        this.date_of_registration = date_of_registration
        this.email = email
        this.phone_number = phone_number
        this.county = county
        this.sub_county = sub_county
        this.ward = ward
        this.village = village
        this.other_name1 = other_name1
        this.last_name1 = last_name1
        this.id_number1 = id_number1
        this.gender1 = gender1
        this.date_of_birth1 = date_of_birth1
        this.email1 = email1
        this.phone_number1 = phone_number1
        this.other_name2 = other_name2
        this.last_name2 = last_name2
        this.id_number2 = id_number2
        this.gender2 = gender2
        this.date_of_birth2 = date_of_birth2
        this.email2 = email2
        this.phone_number2 = phone_number2
        this.other_name3 = other_name3
        this.last_name3 = last_name3
        this.id_number3 = id_number3
        this.gender3 = gender3
        this.date_of_birth3 = date_of_birth3
        this.email3 = email3
        this.phone_number3 = phone_number3
        this.user_id = user_id
        this.user_authorised = user_authorised
        this.authorisation_token = authorisation_token
    }
}