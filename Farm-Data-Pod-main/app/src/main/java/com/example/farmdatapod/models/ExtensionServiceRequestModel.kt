package com.example.farmdatapod.models
import com.example.farmdatapod.MarketProduce
import com.example.farmdatapod.ExtScoutingStation
import com.example.farmdatapod.FertilizerUsed
import com.example.farmdatapod.PesticideUsed
import com.example.farmdatapod.ForecastYield

data class ExtensionServiceRequestModel(
    val ext_scouting_stations: List<ExtScoutingStation>,
    val fertlizers_used: List<FertilizerUsed>,
    val field: String,
    val forecast_yields: List<ForecastYield>,
    val gapping: Gapping,
    val harvesting: Harvesting,
    val liming: Liming,
    val marketProduces: List<MarketProduce>,
    val mulching: Mulching,
    val nursery: Nursery,
    val pesticides_used: List<PesticideUsed>,
    val planned_date_of_planting: String,
    val producer: String,
    val prunning_thinning_desuckering: PrunningThinningDesuckering,
    val soil_analysis: SoilAnalysis,
    val transplanting: Transplanting,
    val weeding: Weeding,
    val week_number: Int
)