package com.example.farmdatapod.season.register.registerSeasonData

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.farmdatapod.models.SeasonRequestModel
import com.example.farmdatapod.models.SeasonResponse
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class SeasonRepository(private val context: Context) : SyncableRepository {
    private val TAG = "SeasonRepository"
    private val seasonDao = AppDatabase.getInstance(context).seasonDao()
    private val apiService = RestClient.getApiService(context)

    suspend fun saveSeason(season: Season, isOnline: Boolean): Result<Season> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save season: ${season.season_name}, Online mode: $isOnline")

            val existingSeason = seasonDao.getSeasonByNameAndProducer(season.season_name, season.producer)
            val localId: Long

            if (existingSeason == null) {
                val newSeason = season.copy(
                    lastModified = System.currentTimeMillis(),
                    syncStatus = false
                )
                localId = seasonDao.insertSeason(newSeason)
                Log.d(TAG, "Season saved locally with ID: $localId")
            } else {
                localId = existingSeason.id.toLong()
                Log.d(TAG, "Season already exists locally with ID: ${existingSeason.id}")
                val updatedSeason = existingSeason.copy(
                    planned_date_of_planting = season.planned_date_of_planting,
                    planned_date_of_harvest = season.planned_date_of_harvest,
                    comments = season.comments,
                    lastModified = System.currentTimeMillis(),
                    syncStatus = false
                )
                updateSeasonAndMarkForSync(updatedSeason)
            }

            if (isOnline && isNetworkAvailable()) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val seasonRequest = createSeasonRequest(season)
                    val response = apiService.registerSeasonPlanning(seasonRequest).execute()

                    when {
                        response.isSuccessful -> {
                            response.body()?.let { serverSeason ->
                                seasonDao.markAsSynced(localId.toInt(), serverSeason.id, System.currentTimeMillis())
                                Log.d(TAG, "Season synced with server ID: ${serverSeason.id}")
                            }
                        }
                        response.code() in 400..499 -> {
                            Log.e(TAG, "Client error during sync: ${response.code()}")
                            throw Exception("Client error: ${response.errorBody()?.string()}")
                        }
                        response.code() in 500..599 -> {
                            Log.e(TAG, "Server error during sync: ${response.code()}")
                            throw Exception("Server error")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                    throw e
                }
            } else {
                Log.d(TAG, "Offline mode or no network - Season will sync later")
            }
            Result.success(season)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving season", e)
            Result.failure(e)
        }
    }

    private suspend fun updateSeasonAndMarkForSync(season: Season) {
        withContext(Dispatchers.IO) {
            seasonDao.updateSeason(season)
            seasonDao.markForSync(season.id)
            Log.d(TAG, "Updated season and marked for sync: ${season.season_name}")
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val unsyncedSeasons = seasonDao.getUnsyncedSeasons()
            Log.d(TAG, "Found ${unsyncedSeasons.size} unsynced seasons")

            var successCount = 0
            var failureCount = 0

            // Process in smaller batches to avoid memory issues
            unsyncedSeasons.chunked(20).forEach { batch ->
                batch.forEach { season ->
                    try {
                        val seasonRequest = createSeasonRequest(season)
                        val response = apiService.registerSeasonPlanning(seasonRequest).execute()

                        if (response.isSuccessful) {
                            response.body()?.let { serverSeason ->
                                seasonDao.markAsSynced(season.id, serverSeason.id, System.currentTimeMillis())
                                successCount++
                                Log.d(TAG, "Successfully synced ${season.season_name}")
                            }
                        } else {
                            failureCount++
                            Log.e(TAG, "Failed to sync ${season.season_name}: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        failureCount++
                        Log.e(TAG, "Error syncing ${season.season_name}", e)
                    }
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
            if (!isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            var savedCount = 0

            // Clean duplicates first
            AppDatabase.getInstance(context).runInTransaction {
                launch {
                    try {
                        val duplicates = seasonDao.findDuplicateSeasons()
                        if (duplicates.isNotEmpty()) {
                            val deletedCount = seasonDao.deleteDuplicateSeasons()
                            Log.d(TAG, "Cleaned $deletedCount duplicate season records")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cleaning season duplicates", e)
                    }
                }
            }

            val response = apiService.getAllSeasons().execute()

            when (response.code()) {
                200 -> {
                    val serverSeasons = response.body()
                    if (serverSeasons == null) {
                        Log.d(TAG, "Server returned empty response")
                        return@withContext Result.success(0)
                    }

                    serverSeasons.distinctBy { "${it.season_name}:${it.producer}" }
                        .forEach { serverSeason ->
                            try {
                                processServerSeason(serverSeason)?.let { savedCount++ }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing season ${serverSeason.season_name}", e)
                            }
                        }
                    Result.success(savedCount)
                }
                401, 403 -> {
                    Log.e(TAG, "Authentication error: ${response.code()}")
                    Result.failure(SecurityException("Authentication error"))
                }
                else -> {
                    Log.e(TAG, "Unexpected response: ${response.code()}")
                    Result.failure(RuntimeException("Server error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    private suspend fun processServerSeason(serverSeason: SeasonResponse): Season? {
        val existingSeason = seasonDao.getSeasonByNameAndProducer(serverSeason.season_name, serverSeason.producer)

        return when {
            existingSeason == null -> {
                val localSeason = createLocalSeasonFromServer(serverSeason)
                seasonDao.insertSeason(localSeason)
                Log.d(TAG, "Inserted new season: ${serverSeason.season_name}")
                localSeason
            }
            shouldUpdate(existingSeason, serverSeason) -> {
                val updatedSeason = createUpdatedSeason(existingSeason, serverSeason)
                seasonDao.updateSeason(updatedSeason)
                Log.d(TAG, "Updated existing season: ${serverSeason.season_name}")
                updatedSeason
            }
            else -> {
                Log.d(TAG, "Season ${serverSeason.season_name} is up to date")
                null
            }
        }
    }

    private fun createLocalSeasonFromServer(serverSeason: SeasonResponse): Season {
        return Season(
            serverId = serverSeason.id,
            producer = serverSeason.producer,
            season_name = serverSeason.season_name,
            planned_date_of_planting = serverSeason.planned_date_of_planting,
            planned_date_of_harvest = serverSeason.planned_date_of_harvest,
            comments = serverSeason.comments,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun createUpdatedSeason(existingSeason: Season, serverSeason: SeasonResponse): Season {
        return existingSeason.copy(
            serverId = serverSeason.id,
            producer = serverSeason.producer,
            season_name = serverSeason.season_name,
            planned_date_of_planting = serverSeason.planned_date_of_planting,
            planned_date_of_harvest = serverSeason.planned_date_of_harvest,
            comments = serverSeason.comments,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun shouldUpdate(existingSeason: Season, serverSeason: SeasonResponse): Boolean {
        return existingSeason.serverId != serverSeason.id ||
                existingSeason.producer != serverSeason.producer ||
                existingSeason.planned_date_of_planting != serverSeason.planned_date_of_planting ||
                existingSeason.planned_date_of_harvest != serverSeason.planned_date_of_harvest ||
                existingSeason.comments != serverSeason.comments
    }

    override suspend fun performFullSync(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

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

    private fun createSeasonRequest(season: Season) = SeasonRequestModel(
        comments = season.comments,
        planned_date_of_harvest = season.planned_date_of_harvest,
        planned_date_of_planting = season.planned_date_of_planting,
        producer = season.producer,
        season_name = season.season_name
    )

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // Database query methods
    fun getAllSeasons(): Flow<List<Season>> = seasonDao.getAllSeasons()

    suspend fun getSeasonById(id: Int): Season? = withContext(Dispatchers.IO) {
        seasonDao.getSeasonById(id)
    }

    fun getSeasonsByProducer(producerCode: String): Flow<List<Season>> = flow {
        val formattedProducer = "%($producerCode)%"
        val seasons = seasonDao.getSeasonsByProducerLike(formattedProducer)
        Log.d(TAG, "Seasons for producer '$producerCode': $seasons")
        emit(seasons)
    }.flowOn(Dispatchers.IO)

    suspend fun updateSeason(season: Season) = withContext(Dispatchers.IO) {
        seasonDao.updateSeason(season)
    }

    suspend fun deleteSeason(season: Season) = withContext(Dispatchers.IO) {
        seasonDao.deleteSeason(season)
    }
}