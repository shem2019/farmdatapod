package com.example.farmdatapod.logistics.inputTransfer.data


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InputTransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInputTransfer(inputTransfer: InputTransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInputTransfers(inputTransfers: List<InputTransferEntity>)

    @Update
    suspend fun updateInputTransfer(inputTransfer: InputTransferEntity)

    @Delete
    suspend fun deleteInputTransfer(inputTransfer: InputTransferEntity)

    @Query("SELECT * FROM input_transfers_table")
    fun getAllInputTransfers(): Flow<List<InputTransferEntity>>

    @Query("SELECT * FROM input_transfers_table WHERE id = :id")
    suspend fun getInputTransferById(id: Int): InputTransferEntity?

    @Query("SELECT * FROM input_transfers_table WHERE server_id = :serverId")
    suspend fun getInputTransferByServerId(serverId: Long): InputTransferEntity?

    @Query("SELECT * FROM input_transfers_table WHERE syncStatus = 0")
    fun getUnsyncedInputTransfers(): Flow<List<InputTransferEntity>>

    @Query("UPDATE input_transfers_table SET syncStatus = 1, lastSynced = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM input_transfers_table WHERE id = :id")
    suspend fun deleteInputTransferById(id: Int)

    @Query("DELETE FROM input_transfers_table")
    suspend fun deleteAllInputTransfers()

    @Query("SELECT * FROM input_transfers_table WHERE destination_hub_id = :hubId OR origin_hub_id = :hubId")
    fun getInputTransfersByHub(hubId: Int): Flow<List<InputTransferEntity>>

    @Query("SELECT COUNT(*) FROM input_transfers_table WHERE syncStatus = 0")
    fun getUnsyncedCount(): Flow<Int>
}