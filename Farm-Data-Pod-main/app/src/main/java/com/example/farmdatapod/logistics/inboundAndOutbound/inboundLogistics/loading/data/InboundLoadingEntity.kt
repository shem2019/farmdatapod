package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.data

// InboundLoadingEntity.kt

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inbound_loading_table",
    indices = [
        Index("truck_loading_number", unique = true)
    ]
)
data class InboundLoadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val authorised: Boolean,
    val comment: String,
    val dispatcher: String,
    val from_: String,
    val grn: String,
    val logistician_status: String,
    val seal_number: String,
    val to: String,
    val total_weight: String,
    val truck_loading_number: String,
    // Offline-first fields
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)

// InboundLoadingDao.kt

