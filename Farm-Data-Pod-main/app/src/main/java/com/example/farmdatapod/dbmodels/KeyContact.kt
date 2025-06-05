package com.example.farmdatapod

class KeyContact {
    var id: Int = 0
    var other_name: String? = null
    var last_name: String? = null
    var gender: String? = null
    var role: String? = null
    var date_of_birth: String? = null
    var email: String? = null
    var phone_number: String = ""
    var id_number: Int = 0
    var hub_id: Int? = null
    var buying_center_id: Int? = null

    override fun toString(): String {
        return "KeyContact(id=$id, other_name=$other_name, last_name=$last_name, gender=$gender, role=$role, " +
                "date_of_birth=$date_of_birth, email=$email, phone_number=$phone_number, id_number=$id_number, " +
                "hub_id=$hub_id, buying_center_id=$buying_center_id)"
    }
}