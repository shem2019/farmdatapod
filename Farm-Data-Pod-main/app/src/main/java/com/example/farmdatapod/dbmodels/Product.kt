package com.example.farmdatapod

class Product {
    // Getters and Setters
    var id: Int = 0
    var category: String? = null
    var products_interested_in: String? = null
    var volume_in_kgs: String? = null
    var packaging: String? = null
    var quality: String? = null
    var frequency: String? = null
    var is_offline: Boolean = false
    var individual_customer_id: Int = 0
    var organisation_customer_id: Int = 0

    // Constructors
    constructor()

    constructor(
        id: Int, category: String?, products_interested_in: String?, volume_in_kgs: String?,
        packaging: String?, quality: String?, frequency: String?, is_offline: Boolean,
        individual_customer_id: Int, organisation_customer_id: Int
    ) {
        this.id = id
        this.category = category
        this.products_interested_in = products_interested_in
        this.volume_in_kgs = volume_in_kgs
        this.packaging = packaging
        this.quality = quality
        this.frequency = frequency
        this.is_offline = is_offline
        this.individual_customer_id = individual_customer_id
        this.organisation_customer_id = organisation_customer_id
    }
}