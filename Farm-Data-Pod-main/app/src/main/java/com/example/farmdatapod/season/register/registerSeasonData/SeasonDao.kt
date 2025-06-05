package com.example.farmdatapod.season.register.registerSeasonData

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeason(season: Season): Long

    @Query("SELECT * FROM seasons")
    fun getAllSeasons(): Flow<List<Season>>

    @Query("SELECT * FROM seasons WHERE syncStatus = 0")
    suspend fun getUnsyncedSeasons(): List<Season>

    @Query("UPDATE seasons SET syncStatus = 1, serverId = :serverId, lastSynced = :timestamp WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE seasons SET syncStatus = 0, lastModified = :timestamp WHERE id = :localId")
    suspend fun markForSync(localId: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM seasons WHERE id = :seasonId")
    suspend fun getSeasonById(seasonId: Int): Season?

    @Query("SELECT * FROM seasons WHERE season_name = :seasonName AND producer = :producer LIMIT 1")
    suspend fun getSeasonByNameAndProducer(seasonName: String, producer: String): Season?

    @Update
    suspend fun updateSeason(season: Season): Int

    @Delete
    suspend fun deleteSeason(season: Season)

    @Query("SELECT COUNT(*) FROM seasons WHERE syncStatus = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT * FROM seasons ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSeason(): Season?

    // Find duplicate seasons based on season_name and producer
    @Query("""
        SELECT s1.* 
        FROM seasons s1 
        INNER JOIN (
            SELECT season_name, producer, COUNT(*) as cnt 
            FROM seasons 
            GROUP BY season_name, producer 
            HAVING cnt > 1
        ) s2 
        ON s1.season_name = s2.season_name 
        AND s1.producer = s2.producer
    """)
    suspend fun findDuplicateSeasons(): List<Season>

    // Delete duplicate seasons keeping the oldest entry for each season_name and producer combination
    @Query("""
        DELETE FROM seasons 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM seasons 
            GROUP BY season_name, producer
        )
    """)
    suspend fun deleteDuplicateSeasons(): Int

    // Producer-related queries
    @Query("SELECT * FROM seasons WHERE producer = :producerId")
    suspend fun getSeasonsByProducer(producerId: Int): List<Season>

    @Query("SELECT * FROM seasons WHERE producer = :producerCode")
    suspend fun getSeasonsByProducer(producerCode: String): List<Season>

    @Query("SELECT * FROM seasons WHERE producer LIKE :producerPattern")
    suspend fun getSeasonsByProducerLike(producerPattern: String): List<Season>

    // Get seasons modified after a certain timestamp
    @Query("SELECT * FROM seasons WHERE lastModified > :timestamp")
    suspend fun getSeasonsModifiedAfter(timestamp: Long): List<Season>

    // Get seasons that need syncing in batches
    @Query("SELECT * FROM seasons WHERE syncStatus = 0 ORDER BY lastModified ASC LIMIT :limit")
    suspend fun getUnsyncedSeasonsBatch(limit: Int): List<Season>
}