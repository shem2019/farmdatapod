package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loading_inputs_table",
    indices = [
        Index("server_id", unique = true)
    ]
)
data class LoadingInputEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val server_id: Long,
    val authorised: Boolean,
    val delivery_note_number: String,
    val input: Int,
    val journey_id: Int,
    val quantity_loaded: Int,
    val stop_point_id: Int,
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)