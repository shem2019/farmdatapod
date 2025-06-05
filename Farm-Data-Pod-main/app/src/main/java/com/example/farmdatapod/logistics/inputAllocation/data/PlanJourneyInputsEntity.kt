package com.example.farmdatapod.logistics.inputAllocation.data


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_journey_inputs_table",
    indices = [
        Index("server_id", unique = true)
    ]
)
data class PlanJourneyInputsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val server_id: Long,
    val journey_id: String,
    val stop_point_id: String,
    val input: String,
    val number_of_units: Int,
    val unit_cost: Double,
    val description: String,
    val dn_number: String,
    val authorised: Boolean = false,
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)

