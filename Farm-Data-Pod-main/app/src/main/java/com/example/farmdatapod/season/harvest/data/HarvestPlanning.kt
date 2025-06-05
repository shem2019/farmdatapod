package com.example.farmdatapod.season.harvest.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "harvest_planning")
data class HarvestPlanning(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val field: String,
    val season: String,
    val producer: String,
    val date: String,
    @ColumnInfo(name = "labor_man_days")
    val laborManDays: Int,
    @ColumnInfo(name = "unit_cost_of_labor")
    val unitCostOfLabor: Double,
    @ColumnInfo(name = "start_time")
    val startTime: String,
    @ColumnInfo(name = "end_time")
    val endTime: String,
    @ColumnInfo(name = "weight_per_unit")
    val weightPerUnit: Double,
    @ColumnInfo(name = "price_per_unit")
    val pricePerUnit: Double,
    @ColumnInfo(name = "harvested_units")
    val harvestedUnits: String, //
    @ColumnInfo(name = "number_of_units")
    val numberOfUnits: Int,
    @ColumnInfo(name = "harvested_quality")
    val harvestedQuality: String,
    val comments: String?,
    @ColumnInfo(name = "season_planning_id")
    val seasonPlanningId: Int,
    @ColumnInfo(name = "is_synced")
    var isSynced: Boolean = false,
    @ColumnInfo(name = "server_id")
    var serverId: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()
)



