package com.example.farmdatapod.logistics.inputAllocation.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanJourneyInputsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(planJourneyInput: PlanJourneyInputsEntity): Long

    @Update
    suspend fun update(planJourneyInput: PlanJourneyInputsEntity)

    @Delete
    suspend fun delete(planJourneyInput: PlanJourneyInputsEntity)

    @Query("SELECT * FROM plan_journey_inputs_table")
    fun getAllPlanJourneyInputs(): Flow<List<PlanJourneyInputsEntity>>

    @Query("SELECT * FROM plan_journey_inputs_table WHERE id = :planJourneyInputId")
    fun getPlanJourneyInputById(planJourneyInputId: Int): PlanJourneyInputsEntity?

    @Query("SELECT * FROM plan_journey_inputs_table WHERE journey_id = :journeyId AND stop_point_id = :stopPointId AND input = :input AND dn_number = :dnNumber")
    fun getPlanJourneyInputByFields(
        journeyId: String,
        stopPointId: String,
        input: String,
        dnNumber: String
    ): PlanJourneyInputsEntity?

    @Query("SELECT * FROM plan_journey_inputs_table WHERE syncStatus = 0")
    fun getUnsyncedPlanJourneyInputs(): List<PlanJourneyInputsEntity>

    @Query("UPDATE plan_journey_inputs_table SET syncStatus = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("UPDATE plan_journey_inputs_table SET syncStatus = 0 WHERE id = :id")
    suspend fun markForSync(id: Int)

    @Query("DELETE FROM plan_journey_inputs_table")
    suspend fun deleteAllPlanJourneyInputs()
}