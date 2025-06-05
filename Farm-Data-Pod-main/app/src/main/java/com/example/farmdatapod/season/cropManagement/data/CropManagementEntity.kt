package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "crop_management")
data class CropManagementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: String? = null,
    val producer: String,
    val season: String,
    val field: String,
    val date: String,
    val seasonPlanningId: Int,
    val comments: String,
    var syncStatus: Boolean = false,
    var lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null
)