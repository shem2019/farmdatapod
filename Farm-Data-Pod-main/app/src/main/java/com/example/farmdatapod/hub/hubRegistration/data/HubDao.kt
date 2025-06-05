package com.example.farmdatapod.hub.hubRegistration.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HubDao {
    @Query("SELECT * FROM hubs")
    fun getAllHubs(): Flow<List<Hub>>

    @Query("SELECT * FROM hubs WHERE id = :hubId")
    suspend fun getHubById(hubId: Int): Hub?

    @Query("SELECT * FROM hubs WHERE user_id = :userId")
    fun getHubsByUserId(userId: String): List<Hub>

    @Query("SELECT * FROM hubs WHERE sync_status = 0")
    suspend fun getUnsyncedHubs(): List<Hub>

    @Query("UPDATE hubs SET sync_status = 0 WHERE id = :localId")
    suspend fun markForSync(localId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHub(hub: Hub): Long



    @Update
    suspend fun updateHub(hub: Hub)

    @Delete
    suspend fun deleteHub(hub: Hub)

    @Query("SELECT * FROM hubs WHERE hub_name = :hubName LIMIT 1")
    suspend fun getHubByName(hubName: String): Hub?

    @Query("UPDATE hubs SET server_id = :serverId, sync_status = 1 WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int)
    @Query("SELECT * FROM hubs WHERE hub_name = :name AND hub_code = :code LIMIT 1")
    suspend fun getHubByNameAndCode(name: String, code: String): Hub?

    @Query("""
        SELECT hub_name, hub_code, COUNT(*) as count 
        FROM hubs 
        GROUP BY hub_name, hub_code 
        HAVING COUNT(*) > 1
    """)
    suspend fun findDuplicateHubs(): List<DuplicateHubInfo>

    @Query("DELETE FROM hubs WHERE id NOT IN (SELECT MIN(id) FROM hubs GROUP BY hub_name, hub_code)")
    suspend fun deleteDuplicateHubs(): Int
}

data class DuplicateHubInfo(
    @ColumnInfo(name = "hub_name") val hubName: String,
    @ColumnInfo(name = "hub_code") val hubCode: String,
    @ColumnInfo(name = "count") val count: Int
)
