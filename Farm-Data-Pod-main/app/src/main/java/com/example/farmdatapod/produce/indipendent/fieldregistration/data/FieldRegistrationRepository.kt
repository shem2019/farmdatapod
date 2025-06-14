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
import java.io.IOException

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

                    // This suspend fun returns Response<Void>, so the body is empty.
                    val response = apiService.registerFarmerField(request)

                    if (response.isSuccessful) {
                        // The POST was successful. We can't get an ID back, but we can
                        // mark the local data as synced.
                        Log.d(TAG, "Field registration synced successfully (POST successful)")
                        fieldRegistrationDao.updateFieldRegistrationSyncStatus(
                            registrationId.toInt(),
                            true
                        )
                        fieldRegistrationDao.updateCropsSyncStatus(registrationId.toInt(), true)
                    } else {
                        Log.e(TAG, "Server sync failed with code ${response.code()}: ${response.errorBody()?.string()}")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Network error during server sync", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during server sync", e)
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
                    // This suspend fun returns Response<Void>, so the body is empty.
                    val response = apiService.registerFarmerField(request)

                    if (response.isSuccessful) {
                        // The POST was successful. We can't get an ID back, but we can
                        // mark the local data as synced.
                        Log.d(TAG, "Successfully synced registration ${registration.fieldRegistration.id}")
                        fieldRegistrationDao.updateFieldRegistrationSyncStatus(
                            registration.fieldRegistration.id,
                            true
                        )
                        fieldRegistrationDao.updateCropsSyncStatus(
                            registration.fieldRegistration.id,
                            true
                        )
                        successCount++
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to sync registration ${registration.fieldRegistration.id}: ${response.errorBody()?.string()}")
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
            // This function returns Call<T>, so .execute() IS needed
            val response = apiService.getFarmerFieldRegistrations().execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Unexpected response code: ${response.code()}")
                return@withContext Result.failure(Exception("Server error: ${response.code()}"))
            }

            val serverRegistrations = response.body()
            if (serverRegistrations.isNullOrEmpty()) {
                Log.d(TAG, "Server returned no new registrations")
                return@withContext Result.success(0)
            }

            var savedCount = 0
            serverRegistrations.forEach { serverRegistration ->
                try {
                    // Check if we already have this registration by server_id
                    val existing = fieldRegistrationDao.getFieldRegistrationByServerId(serverRegistration.id)
                    if (existing != null) {
                        // If it exists, we might just need to update its sync status or other fields
                        // For now, we'll just log and skip to prevent duplicates
                        Log.d(TAG, "Registration with server ID ${serverRegistration.id} already exists.")
                        return@forEach
                    }

                    // Convert server response to local entity
                    val fieldRegistration = FieldRegistrationEntity(
                        serverId = serverRegistration.id,
                        producerId = serverRegistration.producer,
                        fieldNumber = serverRegistration.field_number,
                        fieldSize = serverRegistration.field_size,
                        userId = "current_user_id", // Replace with actual user ID
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
                            baselineYield = cropResponse.baseline_yield_last_season,
                            baselineIncome = cropResponse.baseline_income_last_season,
                            baselineCost = cropResponse.baseline_cost_of_production_last_season,
                            sold = cropResponse.sold,
                            syncStatus = true,
                            fieldRegistrationId = 0
                        )
                    }

                    fieldRegistrationDao.insertFieldRegistrationWithCrops(fieldRegistration, crops)
                    savedCount++
                    Log.d(TAG, "Saved new registration from server with ID ${serverRegistration.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving registration with server ID ${serverRegistration.id}", e)
                }
            }
            Result.success(savedCount)
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

    private fun createFieldRegistrationRequest(
        fieldRegistration: FieldRegistrationEntity,
        crops: List<CropEntity>
    ): FarmerFieldRegistrationRequest {
        return FarmerFieldRegistrationRequest(
            producer = fieldRegistration.producerId,
            field_number = fieldRegistration.fieldNumber,
            field_size = fieldRegistration.fieldSize,
            crops = crops.mapNotNull { crop ->
                if (crop.cropName == null || crop.cropVariety == null || crop.datePlanted == null ||
                    crop.dateOfHarvest == null || crop.population == null || crop.baselineYield == null ||
                    crop.baselineIncome == null || crop.baselineCost == null) {
                    null
                } else {
                    Crop(
                        crop_name = crop.cropName,
                        crop_variety = crop.cropVariety,
                        date_planted = crop.datePlanted,
                        date_of_harvest = crop.dateOfHarvest,
                        population = crop.population,
                        baseline_yield_last_season = crop.baselineYield,
                        baseline_income_last_season = crop.baselineIncome,
                        baseline_cost_of_production_last_season = crop.baselineCost,
                        sold = crop.sold ?: false
                    )
                }
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