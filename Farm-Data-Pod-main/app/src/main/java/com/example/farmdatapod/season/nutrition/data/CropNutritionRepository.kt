package com.example.farmdatapod.season.nutrition.data

import com.example.farmdatapod.models.CropNutritionModel


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.NameOfApplicants
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

class CropNutritionRepository(private val context: Context) : SyncableRepository {
    private val TAG = "CropNutritionRepository"
    private val cropNutritionDao = AppDatabase.getInstance(context).cropNutritionDao()

    suspend fun saveCropNutrition(
        cropNutrition: CropNutritionEntity,
        applicants: List<ApplicantEntity>,
        isOnline: Boolean
    ): Result<CropNutritionEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save crop nutrition: ${cropNutrition.product}, Online mode: $isOnline")

            // Insert and get the ID
            var savedNutrition = cropNutrition
            val localId = cropNutritionDao.insertCropNutritionWithApplicants(cropNutrition, applicants)
            savedNutrition = cropNutrition.copy(id = localId)
            Log.d(TAG, "Crop nutrition saved locally with ID: ${savedNutrition.id}")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val nutritionModel = createCropNutritionModel(savedNutrition, applicants)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).planCropNutrition(nutritionModel)
                            .enqueue(object : Callback<CropNutritionModel> {
                                override fun onResponse(
                                    call: Call<CropNutritionModel>,
                                    response: Response<CropNutritionModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverNutrition ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                cropNutritionDao.markAsSynced(
                                                    localId = localId,
                                                    serverId = serverNutrition.id
                                                )
                                                Log.d(TAG, "Crop nutrition synced with server ID: ${serverNutrition.id}")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<CropNutritionModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Crop nutrition will sync later")
            }
            Result.success(savedNutrition)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving crop nutrition", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedNutrition = cropNutritionDao.getUnsyncedCropNutrition()
            Log.d(TAG, "Found ${unsyncedNutrition.size} unsynced crop nutrition records")

            var successCount = 0
            var failureCount = 0

            unsyncedNutrition.forEach { nutrition ->
                try {
                    val applicants = cropNutritionDao.getApplicantsByCropNutritionId(nutrition.id.toLong())
                    val nutritionModel = createCropNutritionModel(nutrition, applicants.first())

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).planCropNutrition(nutritionModel)
                            .enqueue(object : Callback<CropNutritionModel> {
                                override fun onResponse(
                                    call: Call<CropNutritionModel>,
                                    response: Response<CropNutritionModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverNutrition ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                cropNutritionDao.markAsSynced(nutrition.id, serverNutrition.id)
                                                successCount++
                                                Log.d(TAG, "Successfully synced ${nutrition.product}")
                                            }
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync ${nutrition.product}")
                                    }
                                }

                                override fun onFailure(call: Call<CropNutritionModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing ${nutrition.product}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${nutrition.product}", e)
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
            return@withContext suspendCancellableCoroutine { continuation ->
                context?.let { ctx ->
                    RestClient.getApiService(ctx).getCropNutrition()
                        .enqueue(object : Callback<List<CropNutritionModel>> {
                            override fun onResponse(
                                call: Call<List<CropNutritionModel>>,
                                response: Response<List<CropNutritionModel>>
                            ) {
                                if (response.isSuccessful) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        var savedCount = 0
                                        response.body()?.forEach { serverNutrition ->
                                            processServerNutrition(serverNutrition)?.let { savedCount++ }
                                        }
                                        continuation.resume(Result.success(savedCount))
                                    }
                                } else {
                                    continuation.resume(Result.failure(RuntimeException("Server error: ${response.code()}")))
                                }
                            }

                            override fun onFailure(call: Call<List<CropNutritionModel>>, t: Throwable) {
                                Log.e(TAG, "Network error during sync", t)
                                continuation.resume(Result.failure(t))
                            }
                        })
                } ?: continuation.resume(Result.failure(RuntimeException("Context is null")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    private suspend fun processServerNutrition(serverNutrition: CropNutritionModel): CropNutritionEntity? {
        val existingNutrition = cropNutritionDao.getCropNutritionById(serverNutrition.id)

        return when {
            existingNutrition == null -> {
                val localNutrition = createLocalNutritionFromServer(serverNutrition)
                cropNutritionDao.insertCropNutrition(localNutrition)
                Log.d(TAG, "Inserted new crop nutrition: ${serverNutrition.product}")
                localNutrition
            }
            shouldUpdate(existingNutrition, serverNutrition) -> {
                val updatedNutrition = createUpdatedNutrition(existingNutrition, serverNutrition)
                cropNutritionDao.updateCropNutrition(updatedNutrition)
                Log.d(TAG, "Updated existing crop nutrition: ${serverNutrition.product}")
                updatedNutrition
            }
            else -> {
                Log.d(TAG, "Crop nutrition ${serverNutrition.product} is up to date")
                null
            }
        }
    }

    private fun createCropNutritionModel(
        nutrition: CropNutritionEntity,
        applicants: List<ApplicantEntity>
    ): CropNutritionModel {
        // Get the first applicant if exists, otherwise use empty values
        val firstApplicant = applicants.firstOrNull()

        return CropNutritionModel(
            id = nutrition.id,
            time_of_application = nutrition.timeOfApplication,
            weather_condition = nutrition.weatherCondition,
            producer = nutrition.producer,
            season = nutrition.season,
            field = nutrition.field,
            product = nutrition.product,
            category = nutrition.category,
            formulation = nutrition.formulation,
            unit = nutrition.unit,
            number_of_units = nutrition.numberOfUnits?.toLong(), // Keep as Double per model
            cost_per_unit = nutrition.costPerUnit,
            dosage = nutrition.dosage,
            mixing_ratio = nutrition.mixingRatio,
            total_amount_of_water = nutrition.totalWater?.toLong(), // Keep as Double per model
            labor_man_days = nutrition.laborManDays?.toLong(), // Keep as Double per model
            unit_cost_of_labor = nutrition.unitCostOfLabor?.toLong(), // Keep as Double per model
            comments = nutrition.comments ?: "",
            date = nutrition.date,
            season_planning_id = nutrition.season_planning_id,
            name_of_applicants = NameOfApplicants(
                equipment_used = firstApplicant?.equipmentUsed ?: "",
                name = firstApplicant?.name ?: "",
                ppes_used = firstApplicant?.ppesUsed ?: ""
            ),
            lastModified = nutrition.lastModified
        )
    }

    private fun createLocalNutritionFromServer(serverNutrition: CropNutritionModel): CropNutritionEntity {
        return CropNutritionEntity(
            id = 0, // Let Room generate the ID
            timeOfApplication = serverNutrition.time_of_application,
            weatherCondition = serverNutrition.weather_condition,
            producer = serverNutrition.producer,
            season = serverNutrition.season,
            field = serverNutrition.field,
            product = serverNutrition.product,
            category = serverNutrition.category,
            formulation = serverNutrition.formulation,
            unit = serverNutrition.unit,
            numberOfUnits = serverNutrition.number_of_units,
            costPerUnit = serverNutrition.cost_per_unit,
            dosage = serverNutrition.dosage,
            mixingRatio = serverNutrition.mixing_ratio,
            totalWater = serverNutrition.total_amount_of_water?.toDouble(),
            laborManDays = serverNutrition.labor_man_days?.toDouble(),
            unitCostOfLabor = serverNutrition.unit_cost_of_labor?.toDouble(),
            comments = serverNutrition.comments,
            serverId = serverNutrition.id,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis(),
            date = serverNutrition.date // Assuming the server model has a 'date' field
        )
    }

    // Helper method to create applicant entities from server model
    private fun createApplicantsFromServer(
        serverNutrition: CropNutritionModel,
        nutritionId: Int
    ): List<ApplicantEntity> {
        val applicants = mutableListOf<ApplicantEntity>()

        // Iterate through the lists in parallel
        serverNutrition.name_of_applicants.let { nameOfApplicants ->
            if (nameOfApplicants != null) {
                for (i in nameOfApplicants.name.indices) {
                    applicants.add(
                                ApplicantEntity(
                                    id = 0, // Let Room generate the ID
                                    cropNutritionId = nutritionId.toLong(),
                                    name = (nameOfApplicants.name.getOrNull(i) ?: "").toString(),
                                    ppesUsed = (nameOfApplicants.ppes_used.getOrNull(i) ?: "").toString(),
                                    equipmentUsed = (nameOfApplicants.equipment_used.getOrNull(i) ?: "").toString()
                                )
                            )
                }
            }
        }

        return applicants
    }

    private fun shouldUpdate(local: CropNutritionEntity, server: CropNutritionModel): Boolean {
        // Implement your update logic here
        return local.lastModified < server.lastModified
    }

    private fun createUpdatedNutrition(
        existing: CropNutritionEntity,
        server: CropNutritionModel
    ): CropNutritionEntity {
        return existing.copy(
            // Update all relevant fields from server
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    override suspend fun performFullSync(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val uploadResult = syncUnsynced()
            val downloadResult = syncFromServer()

            Result.success(
                SyncStats(
                    uploadedCount = uploadResult.getOrNull()?.uploadedCount ?: 0,
                    uploadFailures = uploadResult.getOrNull()?.uploadFailures ?: 0,
                    downloadedCount = downloadResult.getOrNull() ?: 0,
                    successful = uploadResult.isSuccess && downloadResult.isSuccess
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllCropNutrition(): Flow<List<CropNutritionWithApplicants>> =
        cropNutritionDao.getCropNutritionWithApplicants()

    suspend fun getCropNutritionById(id: Long): CropNutritionEntity? = withContext(Dispatchers.IO) {
        cropNutritionDao.getCropNutritionById(id)
    }

    suspend fun deleteCropNutrition(nutrition: CropNutritionEntity) = withContext(Dispatchers.IO) {
        cropNutritionDao.deleteCropNutrition(nutrition)
    }

    suspend fun updateCropNutrition(nutrition: CropNutritionEntity) = withContext(Dispatchers.IO) {
        cropNutritionDao.updateCropNutrition(nutrition)
    }
}