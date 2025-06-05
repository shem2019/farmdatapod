package com.example.farmdatapod

class Member {
    // Getters and Setters
    var id: Int = 0
    var other_name: String? = null
    var last_name: String? = null
    var gender: String? = null
    var date_of_birth: String? = null
    var email: String? = null
    var phone_number: Long = 0
    var id_number: Int = 0
    var product_involved: String? = null
    var hectorage_registered_under_cig: String? = null
    var cig_id: Int = 0

    // Constructors
    constructor()

    constructor(
        otherName: String?,
        lastName: String?,
        gender: String?,
        dateOfBirth: String?,
        email: String?,
        phoneNumber: String?,
        idNumber: String?,
        productInvolved: String?,
        hectorageRegisteredUnderCIG: String?,
    )

    constructor(
        id: Int, other_name: String?, last_name: String?, gender: String?, date_of_birth: String?,
        email: String?, phone_number: Long, id_number: Int, product_involved: String?,
        hectorage_registered_under_cig: String?, cig_id: Int,
    ) {
        this.id = id
        this.other_name = other_name
        this.last_name = last_name
        this.gender = gender
        this.date_of_birth = date_of_birth
        this.email = email
        this.phone_number = phone_number
        this.id_number = id_number
        this.product_involved = product_involved
        this.hectorage_registered_under_cig = hectorage_registered_under_cig
        this.cig_id = cig_id
    }
}