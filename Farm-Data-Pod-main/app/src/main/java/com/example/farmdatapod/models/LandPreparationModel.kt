package com.example.farmdatapod.models

data class LandPreparationModel(
val producer: String,
val season: String,
val field: String,
val dateOfLandPreparation: String,
val methodOfLandPreparation: String,
val coverCrop: CoverCrop? = null, // Optional
val mulching: LandPreparationMulching? = null, // Optional
val soilAnalysis: LandPreparationSoilAnalysis? = null, // Optional
val season_planning_id: Int

)
