package com.example.farmdatapod.logistics.planJourney.data


import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.example.farmdatapod.models.JourneyModel
import com.example.farmdatapod.models.StopPoint
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume



class JourneyRepository(private val context: Context) : SyncableRepository {
    private val TAG = "JourneyRepository"
    private val journeyDao = AppDatabase.getInstance(context).journeyDao()

    suspend fun saveJourney(journey: JourneyEntity, stopPoints: List<StopPointEntity>, isOnline: Boolean): Result<JourneyWithStopPoints> =
        withContext(Dispatchers.IO) {
            try {
                val savedJourneyResult = AppDatabase.getInstance(context).runInTransaction<JourneyWithStopPoints> {
                    // First insert the journey
                    val journeyId = runBlocking { journeyDao.insertJourney(journey) }
                    Log.d(TAG, "Successfully inserted journey with ID: $journeyId")

                    // Then insert stop points with the new journey ID
                    val updatedStopPoints = stopPoints.map {
                        it.copy(journey_id = journeyId)
                    }

                    try {
                        runBlocking { journeyDao.insertStopPoints(updatedStopPoints) }
                        Log.d(TAG, "Successfully inserted ${updatedStopPoints.size} stop points")
                    } catch (e: SQLiteConstraintException) {
                        Log.e(TAG, "Failed to insert stop points: ${e.message}")
                        throw e
                    }

                    JourneyWithStopPoints(journey.copy(id = journeyId), updatedStopPoints)
                }?.let { Result.success(it) } ?: Result.failure(Exception("Failed to save journey"))

                // If online and local save was successful, sync with server
                if (isOnline && savedJourneyResult.isSuccess) {
                    savedJourneyResult.getOrNull()?.let { journeyWithStops ->
                        syncJourneyWithServer(journeyWithStops)
                    }
                }

                savedJourneyResult
            } catch (e: Exception) {
                Log.e(TAG, "Error saving journey", e)
                Result.failure(e)
            }
        }


    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Clean duplicates first
            cleanDuplicates()

            // Fetch journeys from server
            val serverResponse = suspendCancellableCoroutine<Result<List<JourneyModel>>> { continuation ->
                RestClient.getApiService(context).getJourneys()
                    .enqueue(object : Callback<List<JourneyModel>> {
                        override fun onResponse(
                            call: Call<List<JourneyModel>>,
                            response: Response<List<JourneyModel>>
                        ) {
                            when {
                                response.isSuccessful -> {
                                    continuation.resume(Result.success(response.body() ?: emptyList()))
                                }
                                response.code() in listOf(401, 403) -> {
                                    continuation.resume(Result.failure(SecurityException("Authentication error: ${response.code()}")))
                                }
                                else -> {
                                    continuation.resume(Result.failure(RuntimeException("Server error: ${response.code()}")))
                                }
                            }
                        }

                        override fun onFailure(call: Call<List<JourneyModel>>, t: Throwable) {
                            continuation.resume(Result.failure(t))
                        }
                    })
            }

            // Process server response
            serverResponse.fold(
                onSuccess = { journeys ->
                    var savedCount = 0
                    AppDatabase.getInstance(context).runInTransaction {
                        journeys.distinctBy { "${it.date_and_time}:${it.driver}:${it.truck}:${it.route_id}" }
                            .forEach { serverJourney ->
                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        processServerJourney(serverJourney)?.let { savedCount++ }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing journey ${serverJourney.route_id}", e)
                                }
                            }
                    }
                    Result.success(savedCount)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    private suspend fun makeJourneyApiCall(journeyModel: JourneyModel): Result<JourneyModel> {
        return suspendCancellableCoroutine { continuation ->
            RestClient.getApiService(context).planJourney(journeyModel)
                .enqueue(object : Callback<JourneyModel> {
                    override fun onResponse(
                        call: Call<JourneyModel>,
                        response: Response<JourneyModel>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                continuation.resume(Result.success(it))
                            } ?: continuation.resume(Result.failure(Exception("Empty response body")))
                        } else {
                            continuation.resume(Result.failure(Exception("API Error: ${response.code()}")))
                        }
                    }

                    override fun onFailure(call: Call<JourneyModel>, t: Throwable) {
                        continuation.resume(Result.failure(t))
                    }
                })
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedJourneys = journeyDao.getAllJourneysWithStopPoints().filter { !it.journey.syncStatus }
            Log.d(TAG, "Found ${unsyncedJourneys.size} unsynced journeys")

            var successCount = 0
            var failureCount = 0

            unsyncedJourneys.forEach { journeyWithStops ->
                try {
                    val journeyModel = createJourneyModel(journeyWithStops)
                    val apiResult = makeJourneyApiCall(journeyModel)

                    apiResult.onSuccess { serverJourney ->
                        runBlocking {
                            journeyDao.updateJourneySyncStatus(
                                journeyWithStops.journey.id,
                                true,
                                System.currentTimeMillis(),
                                serverJourney.route_id.toLong()
                            )
                        }
                        successCount++
                        Log.d(TAG, "Successfully synced journey ${journeyWithStops.journey.id}")
                    }.onFailure { error ->
                        failureCount++
                        Log.e(TAG, "Failed to sync journey ${journeyWithStops.journey.id}", error)
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing journey ${journeyWithStops.journey.id}", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    private suspend fun syncJourneyWithServer(journeyWithStops: JourneyWithStopPoints) {
        try {
            Log.d(TAG, "Attempting to sync with server...")
            val journeyModel = createJourneyModel(journeyWithStops)

            val apiResult = makeJourneyApiCall(journeyModel)
            apiResult.onSuccess { serverJourney ->
                journeyDao.updateJourneySyncStatus(
                    journeyWithStops.journey.id,
                    true,
                    System.currentTimeMillis(),
                    serverJourney.route_id.toLong()
                )
                Log.d(TAG, "Journey synced with server ID: ${serverJourney.route_id}")
            }.onFailure { error ->
                Log.e(TAG, "Sync failed", error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during server sync", e)
        }
    }

    private suspend fun processServerJourney(serverJourney: JourneyModel): JourneyWithStopPoints? {
        return withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).runInTransaction<JourneyWithStopPoints?> {
                // Check if journey exists by server ID
                val existingJourney = runBlocking {
                    journeyDao.getJourneyByServerId(serverJourney.id)
                }

                if (existingJourney == null) {
                    // Use createLocalJourneyFromServer for new journeys
                    val journeyWithStops = createLocalJourneyFromServer(serverJourney)
                    val journeyId = runBlocking { journeyDao.insertJourney(journeyWithStops.journey) }

                    val updatedStopPoints = journeyWithStops.stopPoints.map {
                        it.copy(journey_id = journeyId)
                    }
                    runBlocking { journeyDao.insertStopPoints(updatedStopPoints) }

                    journeyWithStops.copy(journey = journeyWithStops.journey.copy(id = journeyId))
                } else {
                    // Check if update is needed
                    if (shouldUpdate(existingJourney, serverJourney)) {
                        // Create and save updated version
                        val updatedJourney = createUpdatedJourney(existingJourney, serverJourney)
                        runBlocking { journeyDao.updateJourneyWithStopPoints(updatedJourney) }
                        Log.d(TAG, "Updated existing journey: ${serverJourney.id}")
                        updatedJourney
                    } else {
                        Log.d(TAG, "Journey ${serverJourney.id} is up to date")
                        existingJourney
                    }
                }
            }
        }
    }

    private suspend fun cleanDuplicates() {
        try {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context).runInTransaction {
                    val duplicates = runBlocking { journeyDao.findDuplicateJourneys() }
                    if (duplicates.isNotEmpty()) {
                        val deletedCount = runBlocking { journeyDao.deleteDuplicateJourneys() }
                        Log.d(TAG, "Cleaned $deletedCount duplicate journey records")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning duplicates", e)
        }
    }




    private fun createLocalJourneyFromServer(serverJourney: JourneyModel): JourneyWithStopPoints {
        val journey = JourneyEntity(
            server_id = serverJourney.id,  // Add this to track server's ID
            date_and_time = serverJourney.date_and_time,
            driver = serverJourney.driver,
            logistician_status = serverJourney.logistician_status,
            route_id = serverJourney.route_id,
            truck = serverJourney.truck,
            user_id = serverJourney.user_id,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )

        val stopPoints = serverJourney.stop_points.map { serverStop ->
            StopPointEntity(
                description = serverStop.description,
                purpose = serverStop.purpose,
                stop_point = serverStop.stop_point,
                time = serverStop.time,
                syncStatus = true,
                lastModified = System.currentTimeMillis(),
                lastSynced = System.currentTimeMillis(),
                journey_id = journey.id
            )
        }

        return JourneyWithStopPoints(journey, stopPoints)
    }

    private fun shouldUpdate(existingJourney: JourneyWithStopPoints, serverJourney: JourneyModel): Boolean {
        return existingJourney.journey.date_and_time != serverJourney.date_and_time ||
                existingJourney.journey.driver != serverJourney.driver ||
                existingJourney.journey.logistician_status != serverJourney.logistician_status ||
                existingJourney.journey.truck != serverJourney.truck ||
                existingJourney.stopPoints.size != serverJourney.stop_points.size
    }

    private fun createUpdatedJourney(existingJourney: JourneyWithStopPoints, serverJourney: JourneyModel): JourneyWithStopPoints {
        val updatedJourneyEntity = existingJourney.journey.copy(
            date_and_time = serverJourney.date_and_time,
            driver = serverJourney.driver,
            logistician_status = serverJourney.logistician_status,
            route_id = serverJourney.route_id,
            truck = serverJourney.truck,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )

        val updatedStopPoints = serverJourney.stop_points.map { serverStop ->
            StopPointEntity(
                journey_id = existingJourney.journey.id,
                description = serverStop.description,
                purpose = serverStop.purpose,
                stop_point = serverStop.stop_point,
                time = serverStop.time,
                syncStatus = true,
                lastModified = System.currentTimeMillis(),
                lastSynced = System.currentTimeMillis()
            )
        }

        return JourneyWithStopPoints(updatedJourneyEntity, updatedStopPoints)
    }

    private fun createJourneyModel(journeyWithStops: JourneyWithStopPoints): JourneyModel {
        return JourneyModel(
            id = journeyWithStops.journey.id,
            date_and_time = journeyWithStops.journey.date_and_time,
            driver = journeyWithStops.journey.driver,
            logistician_status = journeyWithStops.journey.logistician_status,
            route_id = journeyWithStops.journey.route_id,
            truck = journeyWithStops.journey.truck,
            user_id = journeyWithStops.journey.user_id, // Add user_id here
            stop_points = journeyWithStops.stopPoints.map { stopPoint ->
                StopPoint(
                    description = stopPoint.description,
                    purpose = stopPoint.purpose,
                    stop_point = stopPoint.stop_point,
                    time = stopPoint.time
                )
            }
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


    suspend fun getJourneyNamesAndIds(): Result<List<JourneyBasicInfo>> = withContext(Dispatchers.IO) {
        try {
            val journeyInfo = journeyDao.getJourneyNamesAndIds()
            Result.success(journeyInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching journey names and IDs", e)
            Result.failure(e)
        }
    }
    suspend fun getJourneysWithStopPointInfo(): Result<List<JourneyStopPointInfo>> = withContext(Dispatchers.IO) {
        try {
            val journeyStopPointInfo = journeyDao.getJourneysWithStopPointInfo()
            Result.success(journeyStopPointInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching journey and stop point info", e)
            Result.failure(e)
        }
    }
    suspend fun getJourneyNamesMap(): Result<Map<Long, String>> = withContext(Dispatchers.IO) {
        try {
            val journeyInfo = journeyDao.getJourneyNamesAndIds()
            val journeyMap = journeyInfo.associate { it.journey_id to it.journey_name }
            Result.success(journeyMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating journey names map", e)
            Result.failure(e)
        }
    }

    // Local database operations
    fun getAllJourneys(): Flow<List<JourneyWithStopPoints>> = journeyDao.getAllJourneysWithStopPointsFlow()

    suspend fun getJourneyById(id: Long): JourneyWithStopPoints? = withContext(Dispatchers.IO) {
        journeyDao.getJourneyWithStopPoints(id)
    }

    suspend fun updateJourney(journeyWithStops: JourneyWithStopPoints) = withContext(Dispatchers.IO) {
        journeyDao.updateJourneyWithStopPoints(journeyWithStops)
    }

    suspend fun deleteJourney(journey: JourneyEntity) = withContext(Dispatchers.IO) {
        journeyDao.deleteJourneyById(journey.id)
    }
}