package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dispatch_table",
    indices = [
        Index("server_id", unique = true)
    ]
)
data class DispatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val server_id: Long,
    val confirm_seal: Boolean,
    val dns: String,
    val documentation: String,
    val journey_id: Int,
    val logistician_status: String,
    val starting_fuel: Double,
    val starting_mileage: Double,
    val time_of_departure: String,
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)