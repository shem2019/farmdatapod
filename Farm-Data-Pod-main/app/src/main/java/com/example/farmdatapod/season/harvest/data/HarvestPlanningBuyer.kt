package com.example.farmdatapod.season.harvest.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "harvest_planning_buyer",
    foreignKeys = [
        ForeignKey(
            entity = HarvestPlanning::class,
            parentColumns = ["id"],
            childColumns = ["harvest_planning_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("harvest_planning_id")]
)
data class HarvestPlanningBuyer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "harvest_planning_id")
    val harvestPlanningId: Long,
    val name: String,
    @ColumnInfo(name = "contact_info")
    val contactInfo: String,
    val quantity: Int,
    @ColumnInfo(name = "is_synced")
    var isSynced: Boolean = false,
    @ColumnInfo(name = "server_id")
    var serverId: Int? = null
)