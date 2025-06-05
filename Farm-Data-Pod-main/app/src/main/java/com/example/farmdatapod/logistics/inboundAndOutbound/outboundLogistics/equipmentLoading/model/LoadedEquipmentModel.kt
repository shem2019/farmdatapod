package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.model

data class LoadedEquipmentModel(
    val authorised: Boolean,
    val delivery_note_number: String,
    val equipment: String,
    val journey_id: Int,
    val quantity_loaded: Int,
    val stop_point_id: Int
)