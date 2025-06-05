package com.example.farmdatapod

import com.google.gson.JsonObject

class ExtensionService(
    var id: Int,
    val producer: String,
    val fieldName: String,
    val planned_date_of_planting: String,
    val week_number: Int,
    val nursery: JsonObject? = null,
    val gapping: JsonObject? = null,
    val soil_analysis: JsonObject? = null,
    val liming: JsonObject? = null,
    val transplanting: JsonObject? = null,
    val weeding: JsonObject? = null,
    val prunning_thinning_desuckering: JsonObject? = null,
    val mulching: JsonObject? = null,
    val harvesting: JsonObject? = null,
    var ext_scouting_stations: List<ExtScoutingStation> = emptyList(),
    var pesticides_used: List<PesticideUsed> = emptyList(),
    var fertlizers_used: List<FertilizerUsed> = emptyList(),
    var forecast_yields: List<ForecastYield> = emptyList(),
    var marketProduces: List<MarketProduce> = emptyList(),
    val user_id: String? = null
)