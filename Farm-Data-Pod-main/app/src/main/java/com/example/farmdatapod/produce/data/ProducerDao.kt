package com.example.farmdatapod.produce.data
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProducerDao {
    // Existing insert/update/delete operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducer(producer: ProducerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducers(producers: List<ProducerEntity>)

    @Update
    suspend fun updateProducer(producer: ProducerEntity)

    @Delete
    suspend fun deleteProducer(producer: ProducerEntity)

    // New queries for duplicate handling
    @Query("""
        SELECT farmer_code, COUNT(*) as count 
        FROM producers 
        GROUP BY farmer_code 
        HAVING COUNT(*) > 1
    """)
    suspend fun findDuplicateProducers(): List<DuplicateProducerInfo>

    @Query("""
        DELETE FROM producers 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM producers 
            GROUP BY farmer_code
            HAVING farmer_code IS NOT NULL
        )
    """)
    suspend fun deleteDuplicateProducers(): Int

    // New query to get producer by farmer code
    @Query("SELECT * FROM producers WHERE farmer_code = :farmerCode LIMIT 1")
    suspend fun getProducerByFarmerCode(farmerCode: String): ProducerEntity?

    // Get all farmer codes for duplicate checking
    @Query("SELECT farmer_code FROM producers WHERE farmer_code IS NOT NULL")
    suspend fun getAllFarmerCodes(): List<String>

    // Existing retrieval queries
    @Query("SELECT * FROM producers")
    fun getAllProducers(): Flow<List<ProducerEntity>>

    @Query("SELECT * FROM producers WHERE id = :producerId")
    suspend fun getProducerById(producerId: Int): ProducerEntity?

    @Query("SELECT * FROM producers WHERE server_id = :serverId LIMIT 1")
    suspend fun getProducerByServerId(serverId: Int?): ProducerEntity?

    @Query("SELECT * FROM producers WHERE sync_status = 0")
    suspend fun getUnsyncedProducers(): List<ProducerEntity>

    @Query("SELECT * FROM producers WHERE user_id = :userId")
    suspend fun getProducersByUserId(userId: String): List<ProducerEntity>

    @Query("SELECT * FROM producers WHERE hub_id = :hubId")
    suspend fun getProducersByHub(hubId: Int): List<ProducerEntity>

    @Query("SELECT * FROM producers WHERE buying_center_id = :buyingCenterId")
    suspend fun getProducersByBuyingCenter(buyingCenterId: Int): List<ProducerEntity>

    @Query("SELECT * FROM producers WHERE producer_type = :producerType")
    suspend fun getProducersByType(producerType: String): List<ProducerEntity>

    // Status and sync related queries
    @Query("UPDATE producers SET server_id = :serverId WHERE id = :localId")
    suspend fun updateServerId(localId: Int, serverId: Int)

    @Query("UPDATE producers SET sync_status = :syncStatus WHERE id = :producerId")
    suspend fun updateSyncStatus(producerId: Long, syncStatus: Boolean)

    @Query("""
        UPDATE producers 
        SET sync_status = 1 
        WHERE id IN (:producerIds)
    """)
    suspend fun markProducersAsSynced(producerIds: List<Int>)

    // Search and filtering queries
    @Query("""
        SELECT * FROM producers 
        WHERE other_name LIKE '%' || :searchQuery || '%' 
        OR last_name LIKE '%' || :searchQuery || '%'
        OR id_number LIKE '%' || :searchQuery || '%'
        OR farmer_code LIKE '%' || :searchQuery || '%'
    """)
    suspend fun searchProducers(searchQuery: String): List<ProducerEntity>

    @Query("""
        SELECT * FROM producers 
        WHERE county = :county 
        AND sync_status = :syncStatus
    """)
    suspend fun getProducersByCountyAndSyncStatus(county: String, syncStatus: Boolean): List<ProducerEntity>

    // Cleanup queries
    @Query("DELETE FROM producers")
    suspend fun deleteAllProducers()

    @Query("SELECT COUNT(*) FROM producers")
    suspend fun getProducerCount(): Int

    // Transaction methods
    @Transaction
    suspend fun syncProducers(producers: List<ProducerEntity>) {
        deleteAllProducers()
        insertProducers(producers)
    }

    // New transaction method for safer duplicate cleanup
    @Transaction
    suspend fun cleanupAndInsertProducers(producers: List<ProducerEntity>) {
        val duplicateCount = deleteDuplicateProducers()
        insertProducers(producers)
    }
}

// Data class for duplicate information

data class DuplicateProducerInfo(
    @ColumnInfo(name = "farmer_code")
    val farmerCode: String,
    val count: Int
)