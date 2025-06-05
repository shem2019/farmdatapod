package com.example.farmdatapod

import com.example.farmdatapod.dbmodels.ScoutingStation
import com.google.gson.JsonObject

data class SeasonPlanning(
    var id: Int = 0,
    var producer: String? = null,
    var fieldName: String? = null,
    var planned_date_of_planting: String? = null,
    var week_number: Int = 0,
    var nursery: JsonObject = JsonObject(),
    var gapping: JsonObject = JsonObject(),
    var soil_analysis: JsonObject = JsonObject(),
    var liming: JsonObject = JsonObject(),
    var transplanting: JsonObject = JsonObject(),
    var weeding: JsonObject = JsonObject(),
    var prunning_thinning_desuckering: JsonObject = JsonObject(),
    var mulching: JsonObject = JsonObject(),
    var harvesting: JsonObject = JsonObject(),
    var user_id: String? = null,
    var marketProduces: List<MarketProduce> = emptyList(),
    var plan_nutritions: List<PlanNutrition> = emptyList(),
    var scouting_stations: List<ScoutingStation> = emptyList(),
    var preventative_diseases: List<PreventativeDisease> = emptyList(),
    var preventative_pests: List<PreventativePest> = emptyList(),
    var plan_irrigations: List<PlanIrrigation> = emptyList()
)

