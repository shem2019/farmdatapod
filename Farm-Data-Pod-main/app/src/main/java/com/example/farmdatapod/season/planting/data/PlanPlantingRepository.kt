package com.example.farmdatapod.season.planting.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.MethodOfPlanting
import com.example.farmdatapod.models.PlanPlantingModel
import com.example.farmdatapod.models.PlantingMaterial
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PlanPlantingRepository(private val context: Context) : SyncableRepository {
    private val TAG = "PlanPlantingRepository"
    private val plantingDao = AppDatabase.getInstance(context).plantingPlanDao()
    private val apiService = RestClient.getApiService(context)

    override suspend fun performFullSync(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            // First sync unsynced local data to server
            val uploadResult = syncUnsynced()

            // Then sync data from server
            val downloadResult = syncFromServer()

            // Combine the results
            return@withContext Result.success(
                SyncStats(
                    uploadedCount = uploadResult.getOrNull()?.uploadedCount ?: 0,
                    uploadFailures = uploadResult.getOrNull()?.uploadFailures ?: 0,
                    downloadedCount = downloadResult.getOrNull() ?: 0,
                    successful = uploadResult.isSuccess && downloadResult.isSuccess
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during full sync", e)
            Result.failure(e)
        }
    }

    suspend fun savePlanPlanting(
        plan: PlanPlantingModel,
        isOnline: Boolean
    ): Result<CompletePlantingPlan> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save planting plan for producer: ${plan.producer}, Online mode: $isOnline")

            // Convert to network request
            val planRequest = createPlanRequest(plan)

            Log.d(TAG, "Request payload: ${Gson().toJson(planRequest)}")


            // Convert to entities and save locally
            val planEntity = plan.toEntity()
            val planId = plantingDao.insertPlantingPlan(planEntity)

            val materialEntity = plan.planting_material?.toEntity(planId)
            val methodEntity = plan.method_of_planting?.toEntity(planId)

            if (materialEntity != null) {
                plantingDao.insertPlantingMaterial(materialEntity)
            }
            if (methodEntity != null) {
                plantingDao.insertPlantingMethod(methodEntity)
            }

            val completePlan = plantingDao.getPlantingPlanById(planId)
                ?: throw IllegalStateException("Failed to retrieve saved plan")

            if (isOnline) {
                try {
                    val response = apiService.planPlanting(planRequest).execute()
                    if (response.isSuccessful) {
                        plantingDao.markPlanAsSynced(planId)
                        Log.d(TAG, "Plan synced successfully with server")
                    } else {
                        Log.e(TAG, "Server sync failed: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            }

            Result.success(completePlan)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving planting plan", e)
            Result.failure(e)
        }
    }

    // Helper method to create network request
    private fun createPlanRequest(plan: PlanPlantingModel) = PlanPlantingModel(
        producer = plan.producer,
        field = plan.field,
        season = plan.season,
        date_of_planting = plan.date_of_planting,
        crop = plan.crop,
        crop_population = plan.crop_population,
        target_population = plan.target_population,
        crop_cycle_in_weeks = plan.crop_cycle_in_weeks,
        labor_man_days = plan.labor_man_days,
        unit_cost_of_labor = plan.unit_cost_of_labor,
        season_planning_id = plan.season_planning_id,
        planting_material = plan.planting_material,
        method_of_planting = plan.method_of_planting
    )

    // New query methods
    // In PlanPlantingRepository

    // Change these Flow methods to handle suspend functions correctly:
    // In PlanPlantingRepository

    // Simply return the Flow from DAO directly since it's already a Flow
    fun getPlantingPlansByProducer(producerId: String): Flow<List<CompletePlantingPlan>> =
        plantingDao.getPlantingPlansByProducer(producerId)

    fun getPlantingPlansBySeason(seasonId: Int): Flow<List<CompletePlantingPlan>> =
        plantingDao.getPlantingPlansBySeason(seasonId)

    fun getPlantingPlansByField(fieldName: String): Flow<List<CompletePlantingPlan>> =
        plantingDao.getPlantingPlansByField(fieldName)


    // Extension functions for entity conversions
    private fun PlanPlantingModel.toEntity(syncStatus: Boolean = false) = PlantingPlanEntity(
        producer = producer,
        field = field,
        season = season,
        date_of_planting = date_of_planting,
        crop = crop,
        crop_population = crop_population,
        target_population = target_population,
        crop_cycle_in_weeks = crop_cycle_in_weeks,
        labor_man_days = labor_man_days,
        unit_cost_of_labor = unit_cost_of_labor,
        season_planning_id = season_planning_id,
        sync_status = syncStatus  // Add this parameter
    )

    private fun PlantingMaterial.toEntity(planId: Long) = PlantingMaterialEntity(
        planting_plan_id = planId,
        type = type,
        seed_batch_number = seed_batch_number,
        source = source,
        unit = unit,
        unit_cost = unit_cost
    )

    private fun MethodOfPlanting.toEntity(planId: Long) = PlantingMethodEntity(
        planting_plan_id = planId,
        method = method,
        unit = unit,
        labor_man_days = labor_man_days
    )

    private fun PlantingMaterialEntity.toModel() = PlantingMaterial(
        type = type,
        seed_batch_number = seed_batch_number,
        source = source,
        unit = unit,
        unit_cost = unit_cost
    )

    private fun PlantingMethodEntity.toModel() = MethodOfPlanting(
        method = method,
        unit = unit,
        labor_man_days = labor_man_days
    )

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedPlans = plantingDao.getUnsyncedPlantingPlans().first()
            var successCount = 0
            var failureCount = 0

            unsyncedPlans.forEach { unsyncedPlan ->
                try {
                    val material = plantingDao.getPlantingMaterialForPlan(unsyncedPlan.id)
                    val method = plantingDao.getPlantingMethodForPlan(unsyncedPlan.id)

                    if (material != null && method != null) {
                        val planPlantingModel = PlanPlantingModel(
                            producer = unsyncedPlan.producer,
                            field = unsyncedPlan.field,
                            season = unsyncedPlan.season,
                            date_of_planting = unsyncedPlan.date_of_planting,
                            crop = unsyncedPlan.crop,
                            crop_population = unsyncedPlan.crop_population,
                            target_population = unsyncedPlan.target_population,
                            crop_cycle_in_weeks = unsyncedPlan.crop_cycle_in_weeks,
                            labor_man_days = unsyncedPlan.labor_man_days,
                            unit_cost_of_labor = unsyncedPlan.unit_cost_of_labor,
                            season_planning_id = unsyncedPlan.season_planning_id,
                            planting_material = material.toModel(),
                            method_of_planting = method.toModel()
                        )

                        val response = apiService.planPlanting(planPlantingModel).execute()
                        if (response.isSuccessful) {
                            plantingDao.markPlanAsSynced(unsyncedPlan.id)
                            successCount++
                        } else {
                            failureCount++
                        }
                    } else {
                        failureCount++
                        Log.e(TAG, "Missing material or method for plan ${unsyncedPlan.id}")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing plan ${unsyncedPlan.id}", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPlantingPlans().execute()

            return@withContext when (response.code()) {
                200 -> {
                    val serverPlans = response.body() ?: return@withContext Result.success(0)
                    var savedCount = 0

                    // In syncFromServer():
                    serverPlans.forEach { serverPlan ->
                        try {
                            // Now call toEntity() with syncStatus = true
                            val planEntity = serverPlan.toEntity(syncStatus = true)
                            val planId = plantingDao.insertPlantingPlan(planEntity)

                            val materialEntity = serverPlan.planting_material?.toEntity(planId)
                            val methodEntity = serverPlan.method_of_planting?.toEntity(planId)

                            if (materialEntity != null) {
                                plantingDao.insertPlantingMaterial(materialEntity)
                            }
                            if (methodEntity != null) {
                                plantingDao.insertPlantingMethod(methodEntity)
                            }

                            savedCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving plan for producer ${serverPlan.producer}", e)
                        }
                    }
                    Result.success(savedCount)
                }
                401, 403 -> {
                    Log.e(TAG, "Authentication error: ${response.code()}")
                    Result.failure(Exception("Authentication error"))
                }
                else -> {
                    Log.e(TAG, "Unexpected response: ${response.code()}")
                    Result.failure(Exception("Server error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    fun getPlantingPlansFlow(): Flow<List<CompletePlantingPlan>> = plantingDao.getAllPlantingPlans()

    suspend fun getPlantingPlanById(id: Long): CompletePlantingPlan? = withContext(Dispatchers.IO) {
        plantingDao.getPlantingPlanById(id)
    }
}