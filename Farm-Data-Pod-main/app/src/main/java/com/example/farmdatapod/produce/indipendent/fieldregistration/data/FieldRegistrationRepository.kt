package com.example.farmdatapod.produce.indipendent.fieldregistration.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.Crop
import com.example.farmdatapod.models.FarmerFieldRegistrationRequest
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.withContext

class FieldRegistrationRepository(private val context: Context) : SyncableRepository {
    private val TAG = "FieldRegistrationRepo"
    private val fieldRegistrationDao = AppDatabase.getInstance(context).fieldRegistrationDao()
    private val apiService = RestClient.getApiService(context)

    // Save field registration with optional sync
    suspend fun saveFieldRegistration(
        fieldRegistration: FieldRegistrationEntity,
        crops: List<CropEntity>,
        isOnline: Boolean
    ): Result<FieldRegistrationWithCrops> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving field registration for producer: ${fieldRegistration.producerId}")

            // Save locally first
            val registrationId = fieldRegistrationDao.insertFieldRegistration(fieldRegistration)
            crops.forEach { crop ->
                fieldRegistrationDao.insertCrop(crop.copy(fieldRegistrationId = registrationId.toInt()))
            }

            // Fetch the saved registration with its crops
            val savedRegistration = fieldRegistrationDao.getFieldRegistrationById(registrationId.toInt())
                ?: throw Exception("Failed to retrieve saved registration")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting server sync...")
                    val request = createFieldRegistrationRequest(fieldRegistration, crops)
                    // Direct suspend function call without execute()
                    val response = apiService.registerFarmerField(request)

                    if (response.isSuccessful) {
                        fieldRegistrationDao.updateFieldRegistrationSyncStatus(
                            registrationId.toInt(),
                            true
                        )
                        fieldRegistrationDao.updateCropsSyncStatus(registrationId.toInt(), true)
                        Log.d(TAG, "Field registration synced successfully")
                    } else {
                        Log.e(TAG, "Server sync failed: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            }

            Result.success(savedRegistration)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving field registration", e)
            Result.failure(e)
        }
    }

    // Implementation of SyncableRepository interface
    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedRegistrations = fieldRegistrationDao.getUnsyncedFieldRegistrations()
            Log.d(TAG, "Found ${unsyncedRegistrations.size} unsynced field registrations")

            var successCount = 0
            var failureCount = 0

            unsyncedRegistrations.forEach { registration ->
                try {
                    val request = createFieldRegistrationRequest(
                        registration.fieldRegistration,
                        registration.crops
                    )
                    val response = apiService.registerFarmerField(request)

                    if (response.isSuccessful) {
                        fieldRegistrationDao.updateFieldRegistrationSyncStatus(
                            registration.fieldRegistration.id,
                            true
                        )
                        fieldRegistrationDao.updateCropsSyncStatus(
                            registration.fieldRegistration.id,
                            true
                        )
                        successCount++
                        Log.d(TAG, "Successfully synced registration ${registration.fieldRegistration.id}")
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to sync registration ${registration.fieldRegistration.id}")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing registration ${registration.fieldRegistration.id}", e)
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
            Log.d(TAG, "Starting GET request to fetch field registrations")
            val response = apiService.getFarmerFieldRegistrations().execute()

            when (response.code()) {
                200 -> {
                    val serverRegistrations = response.body()
                    if (serverRegistrations.isNullOrEmpty()) {
                        Log.d(TAG, "Server returned empty response")
                        return@withContext Result.success(0)
                    }

                    var savedCount = 0
                    serverRegistrations.forEach { serverRegistration ->
                        try {
                            // Convert server response to local entity
                            val fieldRegistration = FieldRegistrationEntity(
                                serverId = serverRegistration.id,
                                producerId = serverRegistration.producer,
                                fieldNumber = serverRegistration.field_number,
                                fieldSize = serverRegistration.field_size,
                                userId = "current_user_id", // Get from your auth system
                                syncStatus = true
                            )

                            // Convert crops
                            val crops = serverRegistration.crops.map { cropResponse ->
                                CropEntity(
                                    cropName = cropResponse.crop_name,
                                    cropVariety = cropResponse.crop_variety,
                                    datePlanted = cropResponse.date_planted,
                                    dateOfHarvest = cropResponse.date_of_harvest,
                                    population = cropResponse.population,
                                    baselineYield = cropResponse.baseline_yield,
                                    baselineIncome = cropResponse.baseline_income,
                                    baselineCost = cropResponse.baseline_cost,
                                    syncStatus = true,
                                    fieldRegistrationId = 0 // This will be updated after registration is saved
                                )
                            }

                            // Save to local database
                            fieldRegistrationDao.insertFieldRegistrationWithCrops(fieldRegistration, crops)
                            savedCount++
                            Log.d(TAG, "Saved registration ${serverRegistration.id}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving registration ${serverRegistration.id}", e)
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
                    Result.failure(Exception("Server error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
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

    // Helper function to create API request
    private fun createFieldRegistrationRequest(
        fieldRegistration: FieldRegistrationEntity,
        crops: List<CropEntity>
    ): FarmerFieldRegistrationRequest {
        return FarmerFieldRegistrationRequest(
            producer = fieldRegistration.producerId,
            field_number = fieldRegistration.fieldNumber,
            field_size = fieldRegistration.fieldSize,
            crops = crops.map { crop ->
                Crop(
                    crop_name = crop.cropName,
                    crop_variety = crop.cropVariety,
                    date_planted = crop.datePlanted,
                    date_of_harvest = crop.dateOfHarvest,
                    population = crop.population,
                    baseline_yield = crop.baselineYield,
                    baseline_income = crop.baselineIncome,
                    baseline_cost = crop.baselineCost
                )
            }
        )
    }

    // Convenience methods for UI
    fun getAllFieldRegistrations(): Flow<List<FieldRegistrationWithCrops>> =
        fieldRegistrationDao.getAllFieldRegistrations()

    suspend fun getFieldRegistrationById(id: Int): FieldRegistrationWithCrops? =
        fieldRegistrationDao.getFieldRegistrationById(id)

    fun getFieldRegistrationsByProducer(producerId: String): Flow<List<FieldRegistrationWithCrops>> =
        fieldRegistrationDao.getFieldRegistrationsByProducer(producerId)
}