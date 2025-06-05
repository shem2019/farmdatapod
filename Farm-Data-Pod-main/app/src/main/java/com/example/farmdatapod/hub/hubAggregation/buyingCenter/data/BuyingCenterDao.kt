package com.example.farmdatapod.hub.hubAggregation.buyingCenter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BuyingCenterDao {
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(buyingCenter: BuyingCenterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(buyingCenters: List<BuyingCenterEntity>)

    // Update operations
    @Update
    suspend fun update(buyingCenter: BuyingCenterEntity)

    @Query("UPDATE buying_centers SET sync_status = :status, sync_error = :error, last_sync_attempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: Boolean, error: String? = null, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE buying_centers SET sync_status = :status, server_id = :serverId, sync_error = null, last_sync_attempt = :timestamp WHERE id = :localId")
    suspend fun updateSyncStatusAndServerId(localId: Int, serverId: Int, status: Boolean, timestamp: Long = System.currentTimeMillis())

    // Fetch operations
    @Query("SELECT * FROM buying_centers WHERE deleted = 0 ORDER BY created_at DESC")
    fun getAllBuyingCenters(): Flow<List<BuyingCenterEntity>>

    @Query("SELECT * FROM buying_centers WHERE id = :id AND deleted = 0")
    suspend fun getBuyingCenterById(id: Int): BuyingCenterEntity?

    @Query("SELECT * FROM buying_centers WHERE server_id = :serverId AND deleted = 0")
    suspend fun getBuyingCenterByServerId(serverId: Int): BuyingCenterEntity?

    // Hub-related queries
    @Query("SELECT * FROM buying_centers WHERE hub_id = :hubId AND deleted = 0 ORDER BY buying_center_name")
    fun getBuyingCentersByHubId(hubId: Int): Flow<List<BuyingCenterEntity>>

    @Query("SELECT * FROM buying_centers WHERE hub = :hubName AND deleted = 0 ORDER BY buying_center_name")
    fun getBuyingCentersByHubName(hubName: String?): Flow<List<BuyingCenterEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM buying_centers WHERE hub_id = :hubId AND deleted = 0 LIMIT 1)")
    suspend fun hasBuyingCentersForHub(hubId: Int): Boolean

    // Sync-related queries
    @Query("SELECT * FROM buying_centers WHERE sync_status = :status AND deleted = 0")
    suspend fun getBuyingCentersByStatus(status: Boolean): List<BuyingCenterEntity>

    @Query("SELECT * FROM buying_centers WHERE (sync_status = 0 OR delete_sync_status = 0) AND last_sync_attempt < :timestamp")
    suspend fun getUnsynced(timestamp: Long = System.currentTimeMillis() - 300000): List<BuyingCenterEntity> // 5 minutes threshold

    @Query("SELECT DISTINCT server_id FROM buying_centers WHERE server_id IS NOT NULL AND deleted = 0")
    suspend fun getAllServerIds(): List<Int>

    // Deletion operations
    @Query("UPDATE buying_centers SET deleted = 1, last_modified = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE buying_centers SET deleted = 1, delete_sync_status = 0, last_modified = :timestamp WHERE server_id = :serverId")
    suspend fun softDeleteByServerId(serverId: Int, timestamp: Long = System.currentTimeMillis())

    // Cleanup operations
    @Query("DELETE FROM buying_centers")
    suspend fun deleteAllBuyingCenters()

    @Query("DELETE FROM buying_centers WHERE deleted = 1 AND delete_sync_status = 1 AND last_modified < :timestamp")
    suspend fun cleanupDeletedRecords(timestamp: Long = System.currentTimeMillis() - 2592000000) // 30 days old

    // Statistics and counts
    @Query("SELECT COUNT(*) FROM buying_centers WHERE deleted = 0")
    suspend fun getBuyingCenterCount(): Int

    @Query("SELECT COUNT(*) FROM buying_centers WHERE sync_status = 0 AND deleted = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN sync_status = 1 THEN 1 ELSE 0 END) as synced,
            SUM(CASE WHEN sync_status = 0 THEN 1 ELSE 0 END) as unsynced,
            SUM(CASE WHEN sync_error IS NOT NULL THEN 1 ELSE 0 END) as errors
        FROM buying_centers 
        WHERE deleted = 0
    """)
    suspend fun getSyncStats(): SyncStats

    data class SyncStats(
        val total: Int,
        val synced: Int,
        val unsynced: Int,
        val errors: Int
    )
}