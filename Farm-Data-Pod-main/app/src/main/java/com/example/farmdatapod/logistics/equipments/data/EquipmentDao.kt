package com.example.farmdatapod.logistics.equipments.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipment: EquipmentEntity): Long

    @Update
    suspend fun update(equipment: EquipmentEntity)

    @Delete
    suspend fun delete(equipment: EquipmentEntity)

    @Query("SELECT * FROM equipment_table")
    fun getAllEquipment(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment_table WHERE id = :equipmentId")
    fun getEquipmentById(equipmentId: Int): EquipmentEntity?

    @Query("SELECT * FROM equipment_table WHERE journey_id = :journeyId AND stop_point_id = :stopPointId AND equipment = :equipment AND dn_number = :dnNumber")
    fun getEquipmentByFields(
        journeyId: Int,
        stopPointId: Int,
        equipment: String,
        dnNumber: String
    ): EquipmentEntity?

    @Query("SELECT * FROM equipment_table WHERE syncStatus = 0")
    fun getUnsyncedEquipment(): List<EquipmentEntity>

    @Query("UPDATE equipment_table SET syncStatus = 1, lastSynced = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE equipment_table SET syncStatus = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun markForSync(id: Int, timestamp: Long = System.currentTimeMillis())
}