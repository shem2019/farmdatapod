package com.example.farmdatapod

class MarketProduce {
    var id: Int = 0
    var product: String? = null
    var product_category: String? = null
    var acerage: String? = null
    var season_planning_id: Int? = null // Nullable
    var extension_service_id: Int? = null // Nullable


    fun getDisplayString(): String {
        return product ?: "Unknown"
    }

    fun getValueString(): String {
        return "$id, $product_category"
    }
}

