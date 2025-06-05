package com.example.farmdatapod.models

data class B2CPaymentResponse(
    val ConversationID: String,
    val OriginatorConversationID: String,
    val ResponseCode: String,
    val ResponseDescription: String
)