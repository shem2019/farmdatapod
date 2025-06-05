package com.example.farmdatapod.logistics.inputTransfer.data


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "input_transfers_table",
    indices = [
        Index("server_id", unique = true)
    ]
)
data class InputTransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val server_id: Long,
    val destination_hub_id: Int,
    val input: Int,
    val origin_hub_id: Int,
    val quantity: Int,
    val status: String,
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)