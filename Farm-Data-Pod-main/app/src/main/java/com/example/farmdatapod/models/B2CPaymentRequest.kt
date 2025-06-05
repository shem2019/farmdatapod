package com.example.farmdatapod.models

data class B2CPaymentRequest(
    val InitiatorName: String,
    val SecurityCredential: String,
    val CommandID: String,
    val Amount: Int,
    val PartyA: Int,
    val PartyB: Long,
    val Remarks: String,
    val QueueTimeOutURL: String,
    val ResultURL: String,
    val Occasion: String
)