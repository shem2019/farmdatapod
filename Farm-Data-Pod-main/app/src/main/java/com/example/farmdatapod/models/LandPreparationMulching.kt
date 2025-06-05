package com.example.farmdatapod.models

data class LandPreparationMulching(
    val typeOfMulch: String,
    val costOfMulch: Double,
    val lifeCycleOfMulchInSeasons: Int,
    val manDays: Int,
    val unitCostOfLabor: Double
)