package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.data



import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundOffloadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inboundOffloadingEntity: InboundOffloadingEntity): Long

    @Update
    suspend fun update(inboundOffloadingEntity: InboundOffloadingEntity)

    @Delete
    suspend fun delete(inboundOffloadingEntity: InboundOffloadingEntity)

    @Query("SELECT * FROM inbound_offloading_table ORDER BY lastModified DESC")
    fun getAllInboundOffloadings(): Flow<List<InboundOffloadingEntity>>

    @Query("SELECT * FROM inbound_offloading_table WHERE truck_offloading_number = :truckOffloadingNumber")
    suspend fun getInboundOffloadingByNumber(truckOffloadingNumber: String): InboundOffloadingEntity?

    @Query("SELECT * FROM inbound_offloading_table WHERE id = :id")
    suspend fun getInboundOffloadingById(id: Int): InboundOffloadingEntity?

    @Query("SELECT * FROM inbound_offloading_table WHERE syncStatus = 0")
    fun getUnsyncedInboundOffloadings(): Flow<List<InboundOffloadingEntity>>

    @Query("UPDATE inbound_offloading_table SET syncStatus = 1, lastSynced = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM inbound_offloading_table")
    suspend fun deleteAllInboundOffloadings()
}