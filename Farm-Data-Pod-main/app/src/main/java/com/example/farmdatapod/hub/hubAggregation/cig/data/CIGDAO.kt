package com.example.farmdatapod.hub.hubAggregation.cig.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CIGDAO {
    @Query("SELECT * FROM cigs")
    fun getAllCIGs(): Flow<List<CIG>>

    @Query("SELECT * FROM cigs WHERE sync_status = 0")
    fun getUnsyncedCIGs(): Flow<List<CIG>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCIG(cig: CIG): Long

    @Query("UPDATE cigs SET sync_status = 1, server_id = :serverId WHERE id = :localId")
    suspend fun updateSyncStatus(localId: Int, serverId: Int)

    @Query("DELETE FROM cigs WHERE id = :id")
    suspend fun deleteCIG(id: Int)

    @Query("SELECT * FROM cigs WHERE id = :cigId")
    fun getCIGById(cigId: Int): Flow<CIG?>
}