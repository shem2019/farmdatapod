package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoadingInputDao {

    @Query("SELECT * FROM loading_inputs_table ORDER BY lastModified DESC")
    fun getAllLoadingInputs(): Flow<List<LoadingInputEntity>>

    @Query("SELECT * FROM loading_inputs_table WHERE syncStatus = 0")
    fun getUnsyncedLoadingInputs(): Flow<List<LoadingInputEntity>>

    @Query("SELECT * FROM loading_inputs_table WHERE journey_id = :journeyId")
    fun getLoadingInputsByJourneyId(journeyId: Int): Flow<List<LoadingInputEntity>>

    @Query("SELECT * FROM loading_inputs_table WHERE delivery_note_number = :deliveryNote LIMIT 1")
    suspend fun getLoadingInputByDeliveryNote(deliveryNote: String): LoadingInputEntity?

    @Query("SELECT * FROM loading_inputs_table WHERE server_id = :serverId")
    suspend fun getLoadingInputByServerId(serverId: Long): LoadingInputEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoadingInput(loadingInput: LoadingInputEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoadingInputs(loadingInputs: List<LoadingInputEntity>)
    @Query("UPDATE loading_inputs_table SET syncStatus = :syncStatus, lastSynced = :lastSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, syncStatus: Boolean, lastSynced: Long?)
    @Update
    suspend fun updateLoadingInput(loadingInput: LoadingInputEntity)

    @Query("UPDATE loading_inputs_table SET syncStatus = :syncStatus, lastSynced = :lastSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, syncStatus: Boolean, lastSynced: Long)

    @Delete
    suspend fun deleteLoadingInput(loadingInput: LoadingInputEntity)

    @Query("DELETE FROM loading_inputs_table WHERE syncStatus = 1")
    suspend fun deleteSyncedInputs()

    @Query("SELECT COUNT(*) FROM loading_inputs_table WHERE journey_id = :journeyId")
    suspend fun getLoadingInputCountForJourney(journeyId: Int): Int
}