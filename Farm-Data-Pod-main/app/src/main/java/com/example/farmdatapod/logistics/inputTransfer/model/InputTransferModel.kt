package com.example.farmdatapod.logistics.inputTransfer.model

data class InputTransferModel(
    val destination_hub_id: Int,
    val input: Int,
    val origin_hub_id: Int,
    val quantity: Int,
    val status: String
)