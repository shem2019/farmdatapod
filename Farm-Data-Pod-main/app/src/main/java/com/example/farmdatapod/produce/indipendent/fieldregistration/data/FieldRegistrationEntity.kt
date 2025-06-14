package com.example.farmdatapod.produce.indipendent.fieldregistration.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "field_registrations")
data class FieldRegistrationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,

    @ColumnInfo(name = "producer_id")
    val producerId: String,

    @ColumnInfo(name = "field_number")
    val fieldNumber: Int,

    @ColumnInfo(name = "field_size")
    val fieldSize: Float, // <-- Changed from String to Float

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)