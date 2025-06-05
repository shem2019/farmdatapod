package com.example.farmdatapod.season.scouting.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BaitScoutingDao {
    @Insert
    suspend fun insert(baitScouting: BaitScoutingEntity): Long

    @Insert
    suspend fun insertAll(baitScoutings: List<BaitScoutingEntity>)

    @Update
    suspend fun update(baitScouting: BaitScoutingEntity)

    @Delete
    suspend fun delete(baitScouting: BaitScoutingEntity)

    @Query("SELECT * FROM bait_scouting WHERE is_synced = 0")
    suspend fun getUnsynced(): List<BaitScoutingEntity>

    @Query("UPDATE bait_scouting SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT * FROM bait_scouting")
    suspend fun getAll(): List<BaitScoutingEntity>

    @Query("SELECT * FROM bait_scouting WHERE id = :id")
    suspend fun getById(id: Long): BaitScoutingEntity?

    @Query("SELECT * FROM bait_scouting WHERE producer = :producerCode")
    suspend fun getByProducer(producerCode: String): List<BaitScoutingEntity>

    @Query("SELECT * FROM bait_scouting WHERE season_planning_id = :seasonId")
    suspend fun getBySeason(seasonId: Int): List<BaitScoutingEntity>

    @Query("DELETE FROM bait_scouting WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bait_scouting")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bait_scouting")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM bait_scouting WHERE is_synced = 0")
    suspend fun countUnsynced(): Int
}