package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.model

data class DispatchModel(
    val confirm_seal: Boolean,
    val dns: String,
    val documentation: String,
    val journey_id: Int,
    val logistician_status: String,
    val starting_fuel: Double,
    val starting_mileage: Double,
    val time_of_departure: String
)