package com.example.farmdatapod.season.nursery.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(
    tableName = "inputs",
    foreignKeys = [
        ForeignKey(
            entity = ManagementActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["managementActivityId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InputEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val managementActivityId: Long,
    val input: String,
    val inputCost: Double
)