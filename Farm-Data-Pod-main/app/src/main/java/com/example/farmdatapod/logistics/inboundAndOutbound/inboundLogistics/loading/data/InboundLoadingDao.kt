package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundLoadingDao {


    @Update
    suspend fun update(inboundLoadingEntity: InboundLoadingEntity)

    @Delete
    suspend fun delete(inboundLoadingEntity: InboundLoadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inboundLoadingEntity: InboundLoadingEntity): Long  // Return the row ID

    @Query("SELECT * FROM inbound_loading_table ORDER BY lastModified DESC")
    fun getAllInboundLoadings(): Flow<List<InboundLoadingEntity>>

    @Query("SELECT * FROM inbound_loading_table WHERE truck_loading_number = :truckLoadingNumber")
    suspend fun getInboundLoadingByNumber(truckLoadingNumber: String): InboundLoadingEntity?

    @Query("SELECT * FROM inbound_loading_table WHERE id = :id")
    suspend fun getInboundLoadingById(id: Int): InboundLoadingEntity?

    @Query("SELECT * FROM inbound_loading_table WHERE syncStatus = 0")
    fun getUnsyncedInboundLoadings(): Flow<List<InboundLoadingEntity>>

    @Query("UPDATE inbound_loading_table SET syncStatus = 1, lastSynced = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM inbound_loading_table")
    suspend fun deleteAllInboundLoadings()
}