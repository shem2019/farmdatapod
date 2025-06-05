package com.example.farmdatapod.produce.indipendent.fieldregistration.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crops",
    foreignKeys = [
        ForeignKey(
            entity = FieldRegistrationEntity::class,
            parentColumns = ["id"],
            childColumns = ["field_registration_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("field_registration_id")]
)
data class CropEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "field_registration_id")
    val fieldRegistrationId: Int? = null,

    @ColumnInfo(name = "crop_name")
    val cropName: String? = null,

    @ColumnInfo(name = "crop_variety")
    val cropVariety: String? = null,

    @ColumnInfo(name = "date_planted")
    val datePlanted: String? = null,

    @ColumnInfo(name = "date_of_harvest")
    val dateOfHarvest: String? = null,

    @ColumnInfo(name = "population")
    val population: String? = null,

    @ColumnInfo(name = "baseline_yield_last_season")
    val baselineYield: Double? = null,

    @ColumnInfo(name = "baseline_income_last_season")
    val baselineIncome: String? = null,

    @ColumnInfo(name = "baseline_cost_of_production_last_season")
    val baselineCost: String? = null,

    @ColumnInfo(name = "sync_status")
    val syncStatus: Boolean = false,

    @ColumnInfo(name = "sold")
    val sold: Boolean? = false
)