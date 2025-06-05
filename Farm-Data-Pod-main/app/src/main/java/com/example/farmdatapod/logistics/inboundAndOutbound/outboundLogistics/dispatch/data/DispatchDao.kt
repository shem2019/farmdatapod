package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data


import androidx.room.*


import kotlinx.coroutines.flow.Flow

@Dao
interface DispatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispatch(dispatch: DispatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDispatches(dispatches: List<DispatchEntity>): List<Long>

    @Update
    suspend fun updateDispatch(dispatch: DispatchEntity)

    @Delete
    suspend fun deleteDispatch(dispatch: DispatchEntity)

    @Query("DELETE FROM dispatch_table")
    suspend fun deleteAllDispatches()

    @Query("SELECT * FROM dispatch_table WHERE id = :id")
    suspend fun getDispatchById(id: Int): DispatchEntity?

    @Query("SELECT * FROM dispatch_table WHERE server_id = :serverId")
    suspend fun getDispatchByServerId(serverId: Long): DispatchEntity?

    @Query("SELECT * FROM dispatch_table WHERE journey_id = :journeyId")
    suspend fun getDispatchByJourneyId(journeyId: Int): DispatchEntity?

    @Query("SELECT * FROM dispatch_table ORDER BY lastModified DESC")
    fun getAllDispatches(): Flow<List<DispatchEntity>>

    @Query("SELECT * FROM dispatch_table WHERE syncStatus = 0 ORDER BY lastModified DESC")
    fun getUnsyncedDispatches(): Flow<List<DispatchEntity>>

    @Query("UPDATE dispatch_table SET syncStatus = 1, lastSynced = :timestamp, server_id = :serverId WHERE id = :id")
    suspend fun markAsSynced(id: Int, serverId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM dispatch_table WHERE syncStatus = 0")
    fun getUnsyncedDispatchCount(): Flow<Int>

    @Query("UPDATE dispatch_table SET syncStatus = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun markForSync(id: Int, timestamp: Long = System.currentTimeMillis())
}