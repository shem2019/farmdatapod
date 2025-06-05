package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "equipment_loading_table",
    indices = [
        Index("delivery_note_number", unique = true),
        Index("journey_id"),
        Index("stop_point_id")
    ]
)
data class EquipmentLoadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val server_id: Long,
    val authorised: Boolean,
    val delivery_note_number: String,
    val equipment: String,
    val journey_id: Int,
    val stop_point_id: Int,
    val number_of_units: Int,     // Added for available quantity
    val quantity_loaded: Int = 0,  // This will store how many are being loaded
    val dn_number: String,        // Added for consistency with adapter
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)