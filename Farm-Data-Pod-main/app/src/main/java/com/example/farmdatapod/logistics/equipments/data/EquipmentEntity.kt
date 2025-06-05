package com.example.farmdatapod.logistics.equipments.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "equipment_table",
    indices = [
        Index("dn_number", unique = true)
    ]
)
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val dn_number: String,
    val equipment: String,
    val journey_id: Int,
    val number_of_units: Int,
    val stop_point_id: Int,
    val unit_cost: Int,
    val syncStatus: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)