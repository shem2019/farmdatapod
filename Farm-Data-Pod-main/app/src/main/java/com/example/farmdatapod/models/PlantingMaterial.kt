package com.example.farmdatapod.models

data class PlantingMaterial(
    val type: String,           // "Seed" or "Seedling"
    val seed_batch_number: String?,  // Only for Seed type
    val source: String?,        // Only for Seedling type
    val unit: String,           // Common for both (kg, g, pieces, trays)
    val unit_cost: Int          // Common for both
)