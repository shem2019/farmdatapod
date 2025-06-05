package com.example.farmdatapod

data class BuyingFarmer(
    var id: Int = 0,
    var buying_center: String? = null,
    var producer: String? = null,
    var produce: String? = null,
    var grn_number: String? = null,
    var unit: String? = null,
    var quality: Map<String, Map<String, String>>? = null, // Map<String, Map<String, String>> to handle dynamic keys
    var action: String? = null,
    var weight: Double = 0.0,
    var loaded: Boolean = false,
    var user_id: String? = null
)
