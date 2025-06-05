package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentLoadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipmentLoading: EquipmentLoadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(equipmentLoadings: List<EquipmentLoadingEntity>): List<Long>

    @Update
    suspend fun update(equipmentLoading: EquipmentLoadingEntity)

    @Delete
    suspend fun delete(equipmentLoading: EquipmentLoadingEntity)

    @Query("DELETE FROM equipment_loading_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM equipment_loading_table")
    fun getAllEquipmentLoadings(): Flow<List<EquipmentLoadingEntity>>

    @Query("SELECT * FROM equipment_loading_table WHERE id = :id")
    suspend fun getEquipmentLoadingById(id: Int): EquipmentLoadingEntity?

    @Query("SELECT * FROM equipment_loading_table WHERE journey_id = :journeyId")
    fun getEquipmentLoadingsByJourneyId(journeyId: Int): Flow<List<EquipmentLoadingEntity>>

    @Query("SELECT * FROM equipment_loading_table WHERE journey_id = :journeyId AND stop_point_id = :stopPointId")
    fun getEquipmentLoadingsByJourneyAndStopPoint(journeyId: Int, stopPointId: Int): Flow<List<EquipmentLoadingEntity>>

    @Query("SELECT * FROM equipment_loading_table WHERE syncStatus = 0")
    fun getUnsyncedEquipmentLoadings(): Flow<List<EquipmentLoadingEntity>>

    @Query("UPDATE equipment_loading_table SET syncStatus = :syncStatus, lastSynced = :lastSynced, server_id = :serverId WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, syncStatus: Boolean, lastSynced: Long, serverId: Long)

    @Query("UPDATE equipment_loading_table SET syncStatus = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun markForSync(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM equipment_loading_table WHERE delivery_note_number = :deliveryNoteNumber")
    suspend fun getEquipmentLoadingByDeliveryNote(deliveryNoteNumber: String): EquipmentLoadingEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM equipment_loading_table WHERE delivery_note_number = :deliveryNoteNumber)")
    suspend fun doesDeliveryNoteExist(deliveryNoteNumber: String): Boolean

    @Query("SELECT * FROM equipment_loading_table WHERE stop_point_id = :stopPointId")
    fun getEquipmentLoadingsByStopPoint(stopPointId: Int): Flow<List<EquipmentLoadingEntity>>
}