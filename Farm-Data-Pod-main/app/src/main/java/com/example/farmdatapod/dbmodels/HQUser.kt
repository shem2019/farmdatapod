package com.example.farmdatapod

class HQUser {
    var id: Int = 0
    var other_name: String? = null
    var last_name: String? = null
    var staff_code: String? = null
    var department: String? = null
    var id_number: String? = null
    var gender: String? = null
    var date_of_birth: String? = null
    var email: String? = null
    var phone_number: String? = null
    var education_level: String? = null
    var role: String? = null
    var reporting_to: String? = null
    var related_roles: String? = null
    var is_offline: Int = 0
    var user_id: String? = null // Ensure user_id is a String

    constructor()
    constructor(
        id: Int, other_name: String?, last_name: String?, staff_code: String?, department: String?,
        id_number: String?, gender: String?, date_of_birth: String?, email: String?,
        phone_number: String?, education_level: String?, role: String?, reporting_to: String?,
        related_roles: String?, is_offline: Int, user_id: String?
    ) {
        this.id = id
        this.other_name = other_name
        this.last_name = last_name
        this.staff_code = staff_code
        this.department = department
        this.id_number = id_number
        this.gender = gender
        this.date_of_birth = date_of_birth
        this.email = email
        this.phone_number = phone_number
        this.education_level = education_level
        this.role = role
        this.reporting_to = reporting_to
        this.related_roles = related_roles
        this.is_offline = is_offline
        this.user_id = user_id
    }
}