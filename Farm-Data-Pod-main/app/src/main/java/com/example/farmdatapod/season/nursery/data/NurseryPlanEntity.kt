package com.example.farmdatapod.season.nursery.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date



@Entity(tableName = "nursery_plans")
data class NurseryPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val producer: String,
    val season: String,
    val dateOfEstablishment: String,
    val cropCycleWeeks: Int,
    val crop: String,
    val variety: String,
    val seedBatchNumber: String,
    val typeOfTrays: String,
    val numberOfTrays: Int,
    val comments: String?,
    val seasonPlanningId: Long,
    val createdAt: Date = Date(),
    val isUploaded: Boolean = false
)