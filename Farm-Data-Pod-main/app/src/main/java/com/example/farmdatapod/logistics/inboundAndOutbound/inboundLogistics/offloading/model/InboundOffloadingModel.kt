package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.model

data class InboundOffloadingModel(
    val authorised: Boolean,
    val comment: String,
    val dispatcher: String,
    val grns: String,
    val logistician_status: String,
    val seal_number: String,
    val total_weight: String,
    val truck_offloading_number: String
)