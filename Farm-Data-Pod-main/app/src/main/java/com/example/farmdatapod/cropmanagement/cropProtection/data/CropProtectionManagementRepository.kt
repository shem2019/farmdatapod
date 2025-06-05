package com.example.farmdatapod.cropmanagement.cropProtection.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.CropProtectionModel
import com.example.farmdatapod.models.NameOfApplicants
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.season.cropProtection.data.CropProtectionEntity
import com.example.farmdatapod.season.cropProtection.data.CropProtectionWithApplicants
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

class CropProtectionManagementRepository(private val context: Context) : SyncableRepository {
    private val TAG = "CropProtectionMgmtRepo"
    private val cropProtectionDao = AppDatabase.getInstance(context).cropProtectionDao()

    suspend fun saveCropProtection(
        cropProtection: CropProtectionEntity,
        applicants: List<NameOfApplicants>,
        isOnline: Boolean
    ): Result<CropProtectionEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save crop protection: ${cropProtection.product}, Online mode: $isOnline")

            var savedProtection = cropProtection
            val localId = cropProtectionDao.insertCropProtectionWithApplicants(cropProtection, applicants)
            savedProtection = cropProtection.copy(id = localId)
            Log.d(TAG, "Crop protection saved locally with ID: ${savedProtection.id}")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val protectionModel = createCropProtectionModel(savedProtection, applicants)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).cropProtectionManagement(protectionModel)
                            .enqueue(object : Callback<CropProtectionModel> {
                                override fun onResponse(
                                    call: Call<CropProtectionModel>,
                                    response: Response<CropProtectionModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverProtection ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                cropProtectionDao.markAsSynced(
                                                    id = localId,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                Log.d(TAG, "Crop protection synced with server")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<CropProtectionModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Crop protection will sync later")
            }
            Result.success(savedProtection)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving crop protection", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedProtections = cropProtectionDao.getUnsyncedCropProtections().first()
            Log.d(TAG, "Found ${unsyncedProtections.size} unsynced crop protection records")

            var successCount = 0
            var failureCount = 0

            unsyncedProtections.forEach { protectionWithApplicants ->
                try {
                    val protectionModel = createCropProtectionModel(
                        protectionWithApplicants.cropProtection,
                        protectionWithApplicants.applicants.map {
                            NameOfApplicants(
                                name = it.name,
                                ppes_used = it.ppes_used,
                                equipment_used = it.equipment_used
                            )
                        }
                    )

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).cropProtectionManagement(protectionModel)
                            .enqueue(object : Callback<CropProtectionModel> {
                                override fun onResponse(
                                    call: Call<CropProtectionModel>,
                                    response: Response<CropProtectionModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            cropProtectionDao.markAsSynced(
                                                protectionWithApplicants.cropProtection.id,
                                                System.currentTimeMillis()
                                            )
                                            successCount++
                                            Log.d(TAG, "Successfully synced ${protectionWithApplicants.cropProtection.product}")
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync ${protectionWithApplicants.cropProtection.product}")
                                    }
                                }

                                override fun onFailure(call: Call<CropProtectionModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing ${protectionWithApplicants.cropProtection.product}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${protectionWithApplicants.cropProtection.product}", e)
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
                    RestClient.getApiService(ctx).getCropProtectionManagement()
                        .enqueue(object : Callback<List<CropProtectionModel>> {
                            override fun onResponse(
                                call: Call<List<CropProtectionModel>>,
                                response: Response<List<CropProtectionModel>>
                            ) {
                                if (response.isSuccessful) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        var savedCount = 0
                                        response.body()?.forEach { serverProtection ->
                                            processServerProtection(serverProtection)?.let { savedCount++ }
                                        }
                                        continuation.resume(Result.success(savedCount))
                                    }
                                } else {
                                    continuation.resume(Result.failure(RuntimeException("Server error: ${response.code()}")))
                                }
                            }

                            override fun onFailure(call: Call<List<CropProtectionModel>>, t: Throwable) {
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

    private fun createCropProtectionModel(
        protection: CropProtectionEntity,
        applicants: List<NameOfApplicants>
    ): CropProtectionModel {
        return CropProtectionModel(
            comments = protection.comments ?: "",
            cost_per_unit = protection.costPerUnit,
            date = protection.date,
            dosage = protection.dosage,
            field = protection.field,
            formulation = protection.formulation,
            labor_man_days = protection.laborManDays.toInt(),
            mixing_ratio = protection.mixingRatio,
            name_of_applicants = NameOfApplicants(
                name = applicants.firstOrNull()?.name ?: "",
                ppes_used = applicants.firstOrNull()?.ppes_used ?: "",
                equipment_used = applicants.firstOrNull()?.equipment_used ?: ""
            ),
            number_of_units = protection.numberOfUnits.toInt(),
            producer = protection.producer,
            product = protection.product,
            season = protection.season,
            season_planning_id = protection.season_planning_id.toInt(),
            time_of_application = protection.timeOfApplication,
            total_amount_of_water = protection.totalWater.toInt(),
            unit = protection.unit,
            unit_cost_of_labor = protection.unitCostOfLabor.toInt(),
            weather_condition = protection.weatherCondition,
            who_classification = protection.whoClassification
        )
    }

    private suspend fun processServerProtection(serverProtection: CropProtectionModel): CropProtectionEntity? {
        val existingProtection = cropProtectionDao.getCropProtectionById(serverProtection.id.toLong()).first()

        return when {
            existingProtection == null -> {
                val localProtection = createLocalProtectionFromServer(serverProtection)
                cropProtectionDao.insertCropProtection(localProtection)
                Log.d(TAG, "Inserted new crop protection: ${serverProtection.product}")
                localProtection
            }
            shouldUpdate(existingProtection.cropProtection, serverProtection) -> {
                val updatedProtection = createUpdatedProtection(existingProtection.cropProtection, serverProtection)
                cropProtectionDao.updateCropProtection(updatedProtection)
                Log.d(TAG, "Updated existing crop protection: ${serverProtection.product}")
                updatedProtection
            }
            else -> {
                Log.d(TAG, "Crop protection ${serverProtection.product} is up to date")
                null
            }
        }
    }

    private fun createLocalProtectionFromServer(serverProtection: CropProtectionModel): CropProtectionEntity {
        return CropProtectionEntity(
            timeOfApplication = serverProtection.time_of_application,
            weatherCondition = serverProtection.weather_condition,
            producer = serverProtection.producer,
            season = serverProtection.season,
            field = serverProtection.field,
            product = serverProtection.product,
            whoClassification = serverProtection.who_classification,
            formulation = serverProtection.formulation,
            unit = serverProtection.unit,
            numberOfUnits = serverProtection.number_of_units.toLong(),
            costPerUnit = serverProtection.cost_per_unit,
            dosage = serverProtection.dosage,
            mixingRatio = serverProtection.mixing_ratio,
            totalWater = serverProtection.total_amount_of_water.toDouble(),
            laborManDays = serverProtection.labor_man_days.toDouble(),
            unitCostOfLabor = serverProtection.unit_cost_of_labor.toDouble(),
            comments = serverProtection.comments,
            date = serverProtection.date,
            season_planning_id = serverProtection.season_planning_id.toLong(),
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun shouldUpdate(local: CropProtectionEntity, server: CropProtectionModel): Boolean {
        return local.lastModified < server.lastModified
    }

    private fun createUpdatedProtection(
        existing: CropProtectionEntity,
        server: CropProtectionModel
    ): CropProtectionEntity {
        return existing.copy(
            timeOfApplication = server.time_of_application,
            weatherCondition = server.weather_condition,
            producer = server.producer,
            season = server.season,
            field = server.field,
            product = server.product,
            whoClassification = server.who_classification,
            formulation = server.formulation,
            unit = server.unit,
            numberOfUnits = server.number_of_units.toLong(),
            costPerUnit = server.cost_per_unit,
            dosage = server.dosage,
            mixingRatio = server.mixing_ratio,
            totalWater = server.total_amount_of_water.toDouble(),
            laborManDays = server.labor_man_days.toDouble(),
            unitCostOfLabor = server.unit_cost_of_labor.toDouble(),
            comments = server.comments,
            date = server.date,
            season_planning_id = server.season_planning_id.toLong(),
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis(),
            syncStatus = true
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

    fun getAllCropProtections(): Flow<List<CropProtectionWithApplicants>> =
        cropProtectionDao.getAllCropProtections()

    suspend fun getCropProtectionById(id: Long): Flow<CropProtectionWithApplicants?> =
        cropProtectionDao.getCropProtectionById(id)

    suspend fun deleteCropProtection(protection: CropProtectionEntity) = withContext(Dispatchers.IO) {
        cropProtectionDao.deleteCropProtection(protection)
    }

    suspend fun updateCropProtection(
        protection: CropProtectionEntity,
        applicants: List<NameOfApplicants>
    ) = withContext(Dispatchers.IO) {
        cropProtectionDao.updateCropProtectionWithApplicants(protection, applicants)
    }
}