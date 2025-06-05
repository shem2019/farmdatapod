package com.example.farmdatapod.season.germination.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.GerminationModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class GerminationRepository(private val context: Context) : SyncableRepository {
    private val TAG = "GerminationRepository"
    private val germinationDao = AppDatabase.getInstance(context).germinationDao()
    private val apiService = RestClient.getApiService(context)

    // Save germination with optional sync
    suspend fun saveGermination(germination: GerminationEntity, isOnline: Boolean): Result<GerminationEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to save germination: ${germination.crop}, Online mode: $isOnline")

                // First save locally
                val localId = germinationDao.insert(germination)
                Log.d(TAG, "Germination saved locally with ID: $localId")

                if (isOnline) {
                    try {
                        Log.d(TAG, "Attempting to sync with server...")
                        val germinationRequest = createGerminationModel(germination)
                        val response = apiService.postGermination(germinationRequest).execute()

                        if (response.isSuccessful) {
                            response.body()?.let {
                                germinationDao.markAsSynced(localId)
                                Log.d(TAG, "Germination synced successfully")
                            }
                        } else {
                            Log.e(TAG, "Server sync failed: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during server sync", e)
                    }
                } else {
                    Log.d(TAG, "Offline mode - Germination will sync later")
                }
                Result.success(germination)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving germination", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedData = germinationDao.getUnsyncedGerminationData()
            Log.d(TAG, "Found ${unsyncedData.size} unsynced germination records")

            var successCount = 0
            var failureCount = 0

            unsyncedData.forEach { germination ->
                try {
                    val germinationRequest = createGerminationModel(germination)
                    val response = apiService.postGermination(germinationRequest).execute()

                    if (response.isSuccessful) {
                        germinationDao.markAsSynced(germination.id)
                        successCount++
                        Log.d(TAG, "Successfully synced germination ID: ${germination.id}")
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to sync germination ID: ${germination.id}: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing germination ID: ${germination.id}", e)
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
            Log.d(TAG, "Starting GET request to fetch germination data")
            val response = apiService.getGermination().execute()

            when (response.code()) {
                200 -> {
                    val serverData = response.body()
                    if (serverData == null) {
                        Log.d(TAG, "Server returned empty response")
                        return@withContext Result.success(0)
                    }

                    var savedCount = 0
                    serverData.forEach { serverGermination ->
                        try {
                            val localGermination = convertToEntity(serverGermination)
                            germinationDao.insert(localGermination)
                            savedCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving germination data", e)
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

    // Helper methods for data conversion
    private fun createGerminationModel(entity: GerminationEntity) = GerminationModel(
        crop = entity.crop,
        date_of_germination = entity.dateOfGermination,
        field = entity.field,
        germination_percentage = entity.germinationPercentage,
        labor_man_days = entity.laborManDays,
        producer = entity.producer,
        recommended_management = entity.recommendedManagement,
        season = entity.season,
        season_planning_id = entity.seasonPlanningId,
        status_of_crop = entity.statusOfCrop,
        total_crop_population = entity.totalCropPopulation,
        unit_cost_of_labor = entity.unitCostOfLabor
    )

    private fun convertToEntity(model: GerminationModel) = GerminationEntity(
        crop = model.crop,
        dateOfGermination = model.date_of_germination,
        field = model.field,
        germinationPercentage = model.germination_percentage,
        laborManDays = model.labor_man_days,
        producer = model.producer,
        recommendedManagement = model.recommended_management,
        season = model.season,
        seasonPlanningId = model.season_planning_id,
        statusOfCrop = model.status_of_crop,
        totalCropPopulation = model.total_crop_population,
        unitCostOfLabor = model.unit_cost_of_labor,
        isSynced = true
    )

    // Data access methods
    fun getAllGerminationData(): Flow<List<GerminationEntity>> =
        germinationDao.getAllGerminationData().flowOn(Dispatchers.IO)

    fun getGerminationByProducer(producerCode: String): Flow<List<GerminationEntity>> =
        germinationDao.getGerminationByProducer(producerCode).flowOn(Dispatchers.IO)

    fun getGerminationBySeason(seasonId: String): Flow<List<GerminationEntity>> =
        germinationDao.getGerminationBySeason(seasonId).flowOn(Dispatchers.IO)

    suspend fun getGerminationById(id: Long): GerminationEntity? = withContext(Dispatchers.IO) {
        germinationDao.getGerminationById(id)
    }
}