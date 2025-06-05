package com.example.farmdatapod.season.forecastYields.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.ForecastYieldModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response

class YieldForecastRepository(private val context: Context) : SyncableRepository {
    private val TAG = "YieldForecastRepository"
    private val yieldForecastDao = AppDatabase.getInstance(context).yieldForecastDao()
    private val apiService = RestClient.getApiService(context)

    suspend fun saveYieldForecast(yieldForecast: YieldForecast, isOnline: Boolean): Result<YieldForecast> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Saving yield forecast for season: ${yieldForecast.season}")

                // Save locally first
                val localId = yieldForecastDao.insertYieldForecast(yieldForecast)

                if (isOnline) {
                    try {
                        apiService.planForecastYields(createForecastRequest(yieldForecast))
                            .enqueue(object : retrofit2.Callback<ForecastYieldModel> {
                                override fun onResponse(
                                    call: Call<ForecastYieldModel>,
                                    response: Response<ForecastYieldModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverForecast ->
                                            kotlinx.coroutines.runBlocking {
                                                yieldForecastDao.markAsSynced(localId.toInt(), serverForecast.season_planning_id)
                                            }
                                            Log.d(TAG, "Yield forecast synced with server ID: ${serverForecast.season_planning_id}")
                                        }
                                    } else {
                                        Log.e(TAG, "Server sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<ForecastYieldModel>, t: Throwable) {
                                    Log.e(TAG, "Error during server sync", t)
                                }
                            })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during server sync", e)
                    }
                }

                Result.success(yieldForecast)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving yield forecast", e)
                Result.failure(e)
            }
        }

    // Fix the syncFromServer method in YieldForecastRepository
    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getForecastYields().execute()
            when (response.code()) {
                200 -> {
                    val serverForecasts = response.body()
                    if (serverForecasts == null) {
                        return@withContext Result.success(0)
                    }

                    var savedCount = 0
                    serverForecasts.forEach { serverForecast ->
                        try {
                            val localForecast = YieldForecast(
                                serverId = serverForecast.season_planning_id,
                                seasonPlanningId = serverForecast.season_planning_id,
                                producer = serverForecast.producer,
                                season = serverForecast.season,
                                field = serverForecast.field,
                                date = serverForecast.date,
                                currentCropPopulationPc = serverForecast.current_crop_population_pc,
                                targetYield = serverForecast.target_yield,
                                yieldForecastPc = serverForecast.yield_forecast_pc,
                                forecastQuality = serverForecast.forecast_quality,
                                taComments = serverForecast.ta_comments,
                                syncStatus = true
                            )
                            yieldForecastDao.insertYieldForecast(localForecast)
                            savedCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving forecast", e)
                        }
                    }
                    Result.success(savedCount)
                }
                401, 403 -> Result.failure(Exception("Authentication error: ${response.code()}"))
                else -> Result.failure(Exception("Unexpected response: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    // Fix syncUnsynced to handle responses synchronously
    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedForecasts = yieldForecastDao.getUnsyncedYieldForecasts()
            Log.d(TAG, "Found ${unsyncedForecasts.size} unsynced forecasts")

            var successCount = 0
            var failureCount = 0

            unsyncedForecasts.forEach { forecast ->
                try {
                    val response = apiService.planForecastYields(createForecastRequest(forecast)).execute()
                    if (response.isSuccessful) {
                        response.body()?.let { serverForecast ->
                            yieldForecastDao.markAsSynced(forecast.id, serverForecast.season_planning_id)
                            successCount++
                        }
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to sync forecast: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing forecast", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    suspend fun clearUnsyncedForecasts() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Clearing unsynced forecasts")
            yieldForecastDao.deleteUnsyncedForecasts()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing unsynced forecasts", e)
            Result.failure(e)
        }
    }

    private fun createForecastRequest(forecast: YieldForecast) = ForecastYieldModel(
        current_crop_population_pc = forecast.currentCropPopulationPc,
        date = forecast.date,
        field = forecast.field,
        forecast_quality = forecast.forecastQuality,
        producer = forecast.producer,
        season = forecast.season,
        season_planning_id = forecast.seasonPlanningId,
        ta_comments = forecast.taComments,
        target_yield = forecast.targetYield,
        yield_forecast_pc = forecast.yieldForecastPc
    )

    fun getYieldForecastsBySeasonId(seasonId: Int): Flow<List<YieldForecast>> =
        yieldForecastDao.getYieldForecastsBySeasonId(seasonId)

    fun getAllYieldForecasts(): Flow<List<YieldForecast>> = yieldForecastDao.getAllYieldForecasts()

    override suspend fun performFullSync(): Result<SyncStats> {
        // Implement the logic for full sync here
        return syncUnsynced()
    }
}