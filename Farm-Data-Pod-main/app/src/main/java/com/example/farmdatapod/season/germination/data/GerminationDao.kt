package com.example.farmdatapod.season.germination.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GerminationDao {
    // Create Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(germination: GerminationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(germinations: List<GerminationEntity>)

    // Read Operations
    @Query("SELECT * FROM germination_data ORDER BY createdAt DESC")
    fun getAllGerminationData(): Flow<List<GerminationEntity>>

    @Query("SELECT * FROM germination_data WHERE id = :id")
    suspend fun getGerminationById(id: Long): GerminationEntity?

    @Query("SELECT * FROM germination_data WHERE producer = :producerCode")
    fun getGerminationByProducer(producerCode: String): Flow<List<GerminationEntity>>

    @Query("SELECT * FROM germination_data WHERE season = :seasonId")
    fun getGerminationBySeason(seasonId: String): Flow<List<GerminationEntity>>

    @Query("SELECT * FROM germination_data WHERE isSynced = 0")
    suspend fun getUnsyncedGerminationData(): List<GerminationEntity>

    @Query("""
        SELECT * FROM germination_data 
        WHERE dateOfGermination BETWEEN :startDate AND :endDate 
        ORDER BY dateOfGermination DESC
    """)
    fun getGerminationByDateRange(startDate: String, endDate: String): Flow<List<GerminationEntity>>

    @Query("""
        SELECT * FROM germination_data 
        WHERE producer = :producerCode 
        AND season = :seasonId
    """)
    fun getGerminationByProducerAndSeason(
        producerCode: String,
        seasonId: String
    ): Flow<List<GerminationEntity>>

    // Update Operations
    @Update
    suspend fun update(germination: GerminationEntity)

    @Query("UPDATE germination_data SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE germination_data SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markMultipleAsSynced(ids: List<Long>)

    // Delete Operations
    @Delete
    suspend fun delete(germination: GerminationEntity)

    @Query("DELETE FROM germination_data WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM germination_data WHERE isSynced = 1")
    suspend fun deleteSyncedData()

    @Query("DELETE FROM germination_data")
    suspend fun deleteAll()

    // Count Operations
    @Query("SELECT COUNT(*) FROM germination_data")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM germination_data WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    // Additional Queries
    @Query("""
        SELECT * FROM germination_data 
        WHERE producer = :producerCode 
        AND crop = :cropType
    """)
    fun getGerminationByProducerAndCrop(
        producerCode: String,
        cropType: String
    ): Flow<List<GerminationEntity>>

    @Query("""
        SELECT * FROM germination_data 
        WHERE germinationPercentage < :threshold 
        ORDER BY dateOfGermination DESC
    """)
    fun getLowGerminationRecords(threshold: Int): Flow<List<GerminationEntity>>
}