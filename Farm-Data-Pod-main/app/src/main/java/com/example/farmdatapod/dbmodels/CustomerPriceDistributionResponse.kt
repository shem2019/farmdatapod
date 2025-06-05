package com.example.farmdatapod.dbmodels

import com.example.farmdatapod.CustomerPriceDistribution

data class CustomerPriceDistributionResponse(
    val forms: List<CustomerPriceDistribution>
)