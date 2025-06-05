package com.example.farmdatapod.models

data class CoverCrop(
    val coverCrop: String,
    val dateOfEstablishment: String,
    val unit: String,
    val unitCost: Double,
    val typeOfInoculant: String,
    val dateOfIncorporation: String,
    val manDays: Int,
    val unitCostOfLabor: Double
)