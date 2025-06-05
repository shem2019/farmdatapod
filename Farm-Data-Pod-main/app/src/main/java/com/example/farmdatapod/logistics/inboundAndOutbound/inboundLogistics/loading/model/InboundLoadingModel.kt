package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.model

data class InboundLoadingModel(
    val authorised: Boolean,
    val comment: String,
    val dispatcher: String,
    val from_: String,
    val grn: String,
    val logistician_status: String,
    val seal_number: String,
    val to: String,
    val total_weight: String,
    val truck_loading_number: String
)