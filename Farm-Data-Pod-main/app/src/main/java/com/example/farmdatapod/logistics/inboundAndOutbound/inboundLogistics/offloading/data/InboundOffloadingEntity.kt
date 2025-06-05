package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.data

// InboundOffloadingEntity.kt

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inbound_offloading_table",
    indices = [
        Index("truck_offloading_number", unique = true)
    ]
)
data class InboundOffloadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val authorised: Boolean,
    val comment: String,
    val dispatcher: String,
    val grns: String,
    val logistician_status: String,
    val seal_number: String,
    val total_weight: String,
    val truck_offloading_number: String,
    // Offline-first fields
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)

// InboundOffloadingDao.kt
