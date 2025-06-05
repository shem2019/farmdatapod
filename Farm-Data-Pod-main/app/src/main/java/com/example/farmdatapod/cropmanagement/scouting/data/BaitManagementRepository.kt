package com.example.farmdatapod.cropmanagement.scouting.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.BaitModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.season.scouting.data.BaitScoutingEntity
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class BaitManagementRepository(private val context: Context) : SyncableRepository {
    private val TAG = "BaitManagementRepository"
    private val baitDao = AppDatabase.getInstance(context).baitScoutingDao()
    private val apiService = RestClient.getApiService(context)

    // Save bait scouting with optional sync
    suspend fun saveBaitScouting(bait: BaitScoutingEntity, isOnline: Boolean): Result<BaitScoutingEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to save bait scouting: ${bait.bait_station}, Online mode: $isOnline")

                // First save locally
                val localId = baitDao.insert(bait)
                Log.d(TAG, "Bait scouting saved locally with ID: $localId")

                if (isOnline) {
                    try {
                        Log.d(TAG, "Attempting to sync with server...")
                        val baitRequest = createBaitRequest(bait)
                        val response = apiService.scoutingStationManagement(baitRequest).execute()

                        if (response.isSuccessful) {
                            response.body()?.let { serverBait ->
                                baitDao.markAsSynced(localId)
                                Log.d(TAG, "Bait scouting synced successfully")
                            }
                        } else {
                            Log.e(TAG, "Server sync failed: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during server sync", e)
                    }
                } else {
                    Log.d(TAG, "Offline mode - Bait scouting will sync later")
                }
                Result.success(bait)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving bait scouting", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedBaitScoutings = baitDao.getUnsynced()
            Log.d(TAG, "Found ${unsyncedBaitScoutings.size} unsynced bait scoutings")

            var successCount = 0
            var failureCount = 0

            unsyncedBaitScoutings.forEach { bait ->
                try {
                    val baitRequest = createBaitRequest(bait)
                    val response = apiService.scoutingStationManagement(baitRequest).execute()

                    if (response.isSuccessful) {
                        baitDao.markAsSynced(bait.id)
                        successCount++
                        Log.d(TAG, "Successfully synced bait scouting ${bait.id}")
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to sync bait scouting ${bait.id}: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing bait scouting ${bait.id}", e)
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
            Log.d(TAG, "Starting GET request to fetch bait scoutings")
            val response = apiService.getScoutingStationsManagement().execute()

            when (response.code()) {
                200 -> {
                    val serverBaitScoutings = response.body()
                    if (serverBaitScoutings == null) {
                        Log.d(TAG, "Server returned empty response")
                        return@withContext Result.success(0)
                    }

                    var savedCount = 0
                    serverBaitScoutings.forEach { serverBait ->
                        try {
                            val localBait = BaitScoutingEntity(
                                bait_station = serverBait.bait_station,
                                date = serverBait.date,
                                field = serverBait.field,
                                problem_identified = serverBait.problem_identified,
                                producer = serverBait.producer,
                                season = serverBait.season,
                                season_planning_id = serverBait.season_planning_id,
                                is_synced = true
                            )
                            baitDao.insert(localBait)
                            savedCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving bait scouting ${serverBait.bait_station}", e)
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

    private fun createBaitRequest(bait: BaitScoutingEntity) = BaitModel(
        bait_station = bait.bait_station,
        date = bait.date,
        field = bait.field,
        problem_identified = bait.problem_identified,
        producer = bait.producer,
        season = bait.season,
        season_planning_id = bait.season_planning_id
    )

    fun getAllBaitScoutings(): Flow<List<BaitScoutingEntity>> = flow {
        emit(baitDao.getAll())
    }.flowOn(Dispatchers.IO)

    suspend fun getBaitScoutingById(id: Long): BaitScoutingEntity? = withContext(Dispatchers.IO) {
        baitDao.getById(id)
    }

    fun getBaitScoutingsByProducer(producerCode: String): Flow<List<BaitScoutingEntity>> = flow {
        emit(baitDao.getByProducer(producerCode))
    }.flowOn(Dispatchers.IO)

    fun getBaitScoutingsBySeason(seasonId: Int): Flow<List<BaitScoutingEntity>> = flow {
        emit(baitDao.getBySeason(seasonId))
    }.flowOn(Dispatchers.IO)
}