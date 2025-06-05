package com.example.farmdatapod.season.harvest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface HarvestPlanningDao {
    // Harvest Planning Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHarvestPlanning(harvestPlanning: HarvestPlanning): Long

    @Update
    suspend fun updateHarvestPlanning(harvestPlanning: HarvestPlanning)

    @Delete
    suspend fun deleteHarvestPlanning(harvestPlanning: HarvestPlanning)

    @Query("SELECT * FROM harvest_planning WHERE id = :id")
    suspend fun getHarvestPlanningById(id: Long): HarvestPlanning?

    @Query("SELECT * FROM harvest_planning ORDER BY created_at DESC")
    fun getAllHarvestPlannings(): Flow<List<HarvestPlanning>>

    // Buyer Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuyer(buyer: HarvestPlanningBuyer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuyers(buyers: List<HarvestPlanningBuyer>)

    @Update
    suspend fun updateBuyer(buyer: HarvestPlanningBuyer)

    @Delete
    suspend fun deleteBuyer(buyer: HarvestPlanningBuyer)

    @Query("SELECT * FROM harvest_planning_buyer WHERE harvest_planning_id = :harvestPlanningId")
    suspend fun getBuyersByHarvestPlanningId(harvestPlanningId: Long): List<HarvestPlanningBuyer>

    // Combined Operations
    @Transaction
    @Query("SELECT * FROM harvest_planning WHERE id = :id")
    suspend fun getHarvestPlanningWithBuyers(id: Long): HarvestPlanningWithBuyers?

    @Transaction
    @Query("SELECT * FROM harvest_planning ORDER BY created_at DESC")
    fun getAllHarvestPlanningsWithBuyers(): Flow<List<HarvestPlanningWithBuyers>>

    // Sync Operations
    @Query("SELECT * FROM harvest_planning WHERE is_synced = 0")
    suspend fun getUnsyncedHarvestPlannings(): List<HarvestPlanning>

    @Query("SELECT * FROM harvest_planning_buyer WHERE is_synced = 0")
    suspend fun getUnsyncedBuyers(): List<HarvestPlanningBuyer>

    @Query("UPDATE harvest_planning SET is_synced = 1 WHERE id = :id")
    suspend fun markHarvestPlanningAsSynced(id: Long)

    @Query("UPDATE harvest_planning_buyer SET is_synced = 1 WHERE id = :id")
    suspend fun markBuyerAsSynced(id: Long)

    // Transaction Operations


    @Transaction
    suspend fun updateHarvestPlanningWithBuyers(harvestPlanning: HarvestPlanning, buyers: List<HarvestPlanningBuyer>) {
        updateHarvestPlanning(harvestPlanning)
        // Delete existing buyers not in the new list
        val existingBuyers = getBuyersByHarvestPlanningId(harvestPlanning.id)
        existingBuyers.forEach { existingBuyer ->
            if (!buyers.any { it.id == existingBuyer.id }) {
                deleteBuyer(existingBuyer)
            }
        }
        // Insert or update new buyers
        buyers.forEach { buyer ->
            if (buyer.id == 0L) {
                insertBuyer(buyer.copy(harvestPlanningId = harvestPlanning.id))
            } else {
                updateBuyer(buyer)
            }
        }
    }
    @Transaction
    suspend fun insertHarvestPlanningWithBuyers(harvestPlanning: HarvestPlanning, buyers: List<HarvestPlanningBuyer>): Long {
        val harvestPlanningId = insertHarvestPlanning(harvestPlanning)
        buyers.forEach { buyer ->
            insertBuyer(buyer.copy(harvestPlanningId = harvestPlanningId))
        }
        return harvestPlanningId
    }
    // Cleanup Operations
    @Query("DELETE FROM harvest_planning WHERE id = :id")
    suspend fun deleteHarvestPlanningById(id: Long)

    @Query("DELETE FROM harvest_planning_buyer WHERE harvest_planning_id = :harvestPlanningId")
    suspend fun deleteBuyersByHarvestPlanningId(harvestPlanningId: Long)

    @Transaction
    suspend fun deleteHarvestPlanningWithBuyers(id: Long) {
        deleteBuyersByHarvestPlanningId(id)
        deleteHarvestPlanningById(id)
    }
}

// Type converters for Room
