package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.models

data class LoadingInputModel(
    val authorised: Boolean,
    val delivery_note_number: String,
    val input: String,
    val journey_id: Int,
    val quantity_loaded: Int,
    val stop_point_id: Int
)