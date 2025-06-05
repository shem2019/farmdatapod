package com.example.farmdatapod.models

import com.example.farmdatapod.dbmodels.ScoutingStation
import com.example.farmdatapod.PlanIrrigation
import com.example.farmdatapod.PlanNutrition
import com.example.farmdatapod.PreventativeDisease
import com.example.farmdatapod.PreventativePest
import com.example.farmdatapod.MarketProduce

data class SeasonPlanningRequestModel(
    val `field`: String,
    val gapping: Gapping,
    val harvesting: Harvesting,
    val liming: Liming,
    val marketProduces: List<MarketProduce>,
    val mulching: Mulching,
    val nursery: Nursery,
    val plan_irrigations: List<PlanIrrigation>,
    val plan_nutritions: List<PlanNutrition>,
    val planned_date_of_planting: String,
    val preventative_diseases: List<PreventativeDisease>,
    val preventative_pests: List<PreventativePest>,
    val producer: String,
    val prunning_thinning_desuckering: PrunningThinningDesuckering,
    val scouting_stations: List<ScoutingStation>,
    val soil_analysis: SoilAnalysis,
    val transplanting: Transplanting,
    val weeding: Weeding,
    val week_number: Int
)