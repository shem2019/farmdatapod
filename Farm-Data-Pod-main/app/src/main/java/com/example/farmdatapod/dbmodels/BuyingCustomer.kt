package com.example.farmdatapod

class BuyingCustomer {
    // Getters and Setters
    var id: Int = 0
    var produce: String? = null
    var customer: String? = null
    var grn_number: String? = null
    var unit: String? = null
    var quality: Map<String, Map<String, String>>? = null
    var action: String? = null
    var weight: Double = 0.0
    var online_price: Double = 0.0
    var loaded: Boolean = false
    var user_id: String? = null
}