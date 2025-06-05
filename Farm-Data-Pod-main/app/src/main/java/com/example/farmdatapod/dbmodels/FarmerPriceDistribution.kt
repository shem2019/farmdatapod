package com.example.farmdatapod

data class FarmerPriceDistribution(
    var id: Int = 0,
    var produce_id: Int? = null, // Make it nullable since it can be optional
    var online_price: Float? = null, // Use Float for REAL values
    var hub: String? = null,
    var buying_center: String? = null,
    var unit: String? = null,
    var date: String? = null, // Date as a String, to be parsed into DATETIME
    var comments: String? = null,
    var sold: Boolean = false, // Use Boolean for INTEGER values (0 or 1)
    var user_id: String? = null // UUID as a String
)
