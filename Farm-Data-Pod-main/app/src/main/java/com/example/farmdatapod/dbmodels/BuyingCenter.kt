package com.example.farmdatapod

class BuyingCenter {
    var id: Int = 0
    var hub: String? = null
    var county: String? = null
    var sub_county: String? = null
    var ward: String? = null
    var village: String? = null
    var buying_center_name: String? = null
    var buying_center_code: String? = null
    var address: String? = null
    var year_established: String? = null
    var ownership: String? = null
    var floor_size: String? = null
    var facilities: String? = null
    var input_center: String? = null
    var type_of_building: String? = null
    var location: String? = null
    var user_id: String? = null
    var key_contacts: List<KeyContact>? = null

    override fun toString(): String {
        return "BuyingCenter(id=$id, hub=$hub, county=$county, sub_county=$sub_county, ward=$ward, village=$village, " +
                "buying_center_name=$buying_center_name, buying_center_code=$buying_center_code, address=$address, " +
                "year_established=$year_established, ownership=$ownership, floor_size=$floor_size, facilities=$facilities, " +
                "input_center=$input_center, type_of_building=$type_of_building, location=$location, user_id=$user_id, " +
                "key_contacts=${key_contacts?.joinToString()})"
    }
}
