package com.example.farmdatapod.dbmodels

import com.example.farmdatapod.FarmerPriceDistribution

data class FarmerPriceDistributionResponse(
    val forms: List<FarmerPriceDistribution>
)