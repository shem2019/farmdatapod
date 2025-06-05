package com.example.farmdatapod

data class Loading(
    var id: Int = 0,
    var grn: String? = null,
    var total_weight: String? = null,
    var truck_loading_number: String? = null,
    var from_: String? = null,
    var to: String? = null,
    var comment: String? = null,
    var offloaded: Boolean = false,
    var user_id: String? = null
)