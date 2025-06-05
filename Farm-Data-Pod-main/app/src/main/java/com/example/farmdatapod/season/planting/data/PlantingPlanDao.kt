package com.example.farmdatapod.season.planting.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantingPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlantingPlan(plan: PlantingPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlantingMaterial(material: PlantingMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlantingMethod(method: PlantingMethodEntity)

    @Transaction
    @Query("SELECT * FROM planting_plans ORDER BY created_at DESC")
    fun getAllPlantingPlans(): Flow<List<CompletePlantingPlan>>

    @Transaction
    @Query("SELECT * FROM planting_plans WHERE producer = :producerId ORDER BY created_at DESC")
    fun getPlantingPlansByProducer(producerId: String): Flow<List<CompletePlantingPlan>>

    @Transaction
    @Query("SELECT * FROM planting_plans WHERE season_planning_id = :seasonId ORDER BY created_at DESC")
    fun getPlantingPlansBySeason(seasonId: Int): Flow<List<CompletePlantingPlan>>

    @Transaction
    @Query("SELECT * FROM planting_plans WHERE field = :fieldName ORDER BY created_at DESC")
    fun getPlantingPlansByField(fieldName: String): Flow<List<CompletePlantingPlan>>


    @Transaction
    suspend fun insertFullPlantingPlan(
        plan: PlantingPlanEntity,
        material: PlantingMaterialEntity,
        method: PlantingMethodEntity
    ) {
        val planId = insertPlantingPlan(plan)
        insertPlantingMaterial(material.copy(planting_plan_id = planId))
        insertPlantingMethod(method.copy(planting_plan_id = planId))
    }

    @Query("SELECT * FROM planting_plans WHERE sync_status = 0")
    fun getUnsyncedPlantingPlans(): Flow<List<PlantingPlanEntity>>

    @Query("SELECT * FROM planting_materials WHERE planting_plan_id = :planId")
    suspend fun getPlantingMaterialForPlan(planId: Long): PlantingMaterialEntity?

    @Query("SELECT * FROM planting_methods WHERE planting_plan_id = :planId")
    suspend fun getPlantingMethodForPlan(planId: Long): PlantingMethodEntity?

    @Query("UPDATE planting_plans SET sync_status = 1 WHERE id = :planId")
    suspend fun markPlanAsSynced(planId: Long)

    @Transaction
    @Query("SELECT * FROM planting_plans WHERE id = :id")
    suspend fun getPlantingPlanById(id: Long): CompletePlantingPlan?

    @Query("DELETE FROM planting_plans WHERE id = :planId")
    suspend fun deletePlantingPlan(planId: Long)

    @Query("DELETE FROM planting_materials WHERE planting_plan_id = :planId")
    suspend fun deletePlantingMaterial(planId: Long)

    @Query("DELETE FROM planting_methods WHERE planting_plan_id = :planId")
    suspend fun deletePlantingMethod(planId: Long)

    @Transaction
    suspend fun deleteFullPlantingPlan(planId: Long) {
        deletePlantingPlan(planId)
        deletePlantingMaterial(planId)
        deletePlantingMethod(planId)
    }



}