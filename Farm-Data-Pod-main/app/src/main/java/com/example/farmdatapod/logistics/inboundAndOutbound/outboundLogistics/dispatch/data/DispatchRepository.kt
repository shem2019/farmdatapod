package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.model.DispatchModel
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

class DispatchRepository(private val context: Context) : SyncableRepository {
    private val TAG = "DispatchRepository"
    private val dispatchDao = AppDatabase.getInstance(context).dispatchDao()

    suspend fun saveDispatch(dispatch: DispatchEntity, isOnline: Boolean): Result<DispatchEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save dispatch for journey: ${dispatch.journey_id}, Online mode: $isOnline")

            val existingDispatch = dispatchDao.getDispatchByJourneyId(dispatch.journey_id)
            val localId: Int

            if (existingDispatch == null) {
                localId = dispatchDao.insertDispatch(dispatch).toInt()
                Log.d(TAG, "Dispatch saved locally with ID: $localId")
            } else {
                localId = existingDispatch.id
                Log.d(TAG, "Dispatch already exists locally with ID: ${existingDispatch.id}")
                updateDispatchAndMarkForSync(existingDispatch)
            }

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val dispatchModel = createDispatchModel(dispatch)

                    context.let { ctx ->
                        RestClient.getApiService(ctx).dispatch(dispatchModel)
                            .enqueue(object : Callback<DispatchModel> {
                                override fun onResponse(
                                    call: Call<DispatchModel>,
                                    response: Response<DispatchModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverDispatch ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                dispatchDao.markAsSynced(localId, serverDispatch.journey_id.toLong())
                                                Log.d(TAG, "Dispatch synced with server")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<DispatchModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Dispatch will sync later")
            }
            Result.success(dispatch)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving dispatch", e)
            Result.failure(e)
        }
    }

    private suspend fun updateDispatchAndMarkForSync(dispatch: DispatchEntity) {
        updateDispatch(dispatch)
        markForSync(dispatch.id)
    }

    private suspend fun markForSync(localId: Int) = withContext(Dispatchers.IO) {
        dispatchDao.markForSync(localId)
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedDispatches = dispatchDao.getUnsyncedDispatches().first()
            Log.d(TAG, "Found ${unsyncedDispatches.size} unsynced dispatches")

            var successCount = 0
            var failureCount = 0

            unsyncedDispatches.forEach { dispatch ->
                try {
                    val dispatchModel = createDispatchModel(dispatch)
                    context.let { ctx ->
                        RestClient.getApiService(ctx).dispatch(dispatchModel)
                            .enqueue(object : Callback<DispatchModel> {
                                override fun onResponse(
                                    call: Call<DispatchModel>,
                                    response: Response<DispatchModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverDispatch ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                dispatchDao.markAsSynced(dispatch.id, serverDispatch.journey_id.toLong())
                                                successCount++
                                                Log.d(TAG, "Successfully synced dispatch for journey ${dispatch.journey_id}")
                                            }
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync dispatch for journey ${dispatch.journey_id}")
                                    }
                                }

                                override fun onFailure(call: Call<DispatchModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing dispatch for journey ${dispatch.journey_id}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing dispatch for journey ${dispatch.journey_id}", e)
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
            var savedCount = 0

            return@withContext suspendCancellableCoroutine { continuation ->
                context.let { ctx ->
                    RestClient.getApiService(ctx).getDispatches().enqueue(object : Callback<List<DispatchModel>> {
                        override fun onResponse(
                            call: Call<List<DispatchModel>>,
                            response: Response<List<DispatchModel>>
                        ) {
                            if (response.isSuccessful) {
                                try {
                                    val dispatches = response.body()

                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            AppDatabase.getInstance(context).runInTransaction {
                                                dispatches?.forEach { serverDispatch ->
                                                    try {
                                                        launch {
                                                            processServerDispatch(serverDispatch)?.let { savedCount++ }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Error processing dispatch for journey ${serverDispatch.journey_id}", e)
                                                    }
                                                }
                                            }
                                            continuation.resume(Result.success(savedCount))
                                        } catch (e: Exception) {
                                            continuation.resume(Result.failure(e))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing response", e)
                                    continuation.resume(Result.failure(e))
                                }
                            } else {
                                when (response.code()) {
                                    401, 403 -> {
                                        continuation.resume(Result.failure(SecurityException("Authentication error: ${response.code()}")))
                                    }
                                    else -> {
                                        continuation.resume(Result.failure(RuntimeException("Unexpected response: ${response.code()}")))
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<List<DispatchModel>>, t: Throwable) {
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

    private suspend fun processServerDispatch(serverDispatch: DispatchModel): DispatchEntity? {
        val existingDispatch = dispatchDao.getDispatchByJourneyId(serverDispatch.journey_id)

        return when {
            existingDispatch == null -> {
                val localDispatch = createLocalDispatchFromServer(serverDispatch)
                dispatchDao.insertDispatch(localDispatch)
                Log.d(TAG, "Inserted new dispatch for journey: ${serverDispatch.journey_id}")
                localDispatch
            }
            shouldUpdate(existingDispatch, serverDispatch) -> {
                val updatedDispatch = createUpdatedDispatch(existingDispatch, serverDispatch)
                dispatchDao.updateDispatch(updatedDispatch)
                Log.d(TAG, "Updated existing dispatch for journey: ${serverDispatch.journey_id}")
                updatedDispatch
            }
            else -> {
                Log.d(TAG, "Dispatch for journey ${serverDispatch.journey_id} is up to date")
                null
            }
        }
    }

    private fun createLocalDispatchFromServer(serverDispatch: DispatchModel): DispatchEntity {
        return DispatchEntity(
            server_id = serverDispatch.journey_id.toLong(),
            confirm_seal = serverDispatch.confirm_seal,
            dns = serverDispatch.dns,
            documentation = serverDispatch.documentation,
            journey_id = serverDispatch.journey_id,
            logistician_status = serverDispatch.logistician_status,
            starting_fuel = serverDispatch.starting_fuel,
            starting_mileage = serverDispatch.starting_mileage,
            time_of_departure = serverDispatch.time_of_departure,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun createUpdatedDispatch(existingDispatch: DispatchEntity, serverDispatch: DispatchModel): DispatchEntity {
        return existingDispatch.copy(
            server_id = serverDispatch.journey_id.toLong(),
            confirm_seal = serverDispatch.confirm_seal,
            dns = serverDispatch.dns,
            documentation = serverDispatch.documentation,
            journey_id = serverDispatch.journey_id,
            logistician_status = serverDispatch.logistician_status,
            starting_fuel = serverDispatch.starting_fuel,
            starting_mileage = serverDispatch.starting_mileage,
            time_of_departure = serverDispatch.time_of_departure,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun shouldUpdate(existingDispatch: DispatchEntity, serverDispatch: DispatchModel): Boolean {
        return existingDispatch.confirm_seal != serverDispatch.confirm_seal ||
                existingDispatch.dns != serverDispatch.dns ||
                existingDispatch.documentation != serverDispatch.documentation ||
                existingDispatch.logistician_status != serverDispatch.logistician_status ||
                existingDispatch.starting_fuel != serverDispatch.starting_fuel ||
                existingDispatch.starting_mileage != serverDispatch.starting_mileage ||
                existingDispatch.time_of_departure != serverDispatch.time_of_departure
    }

    private fun createDispatchModel(dispatch: DispatchEntity) = DispatchModel(
        confirm_seal = dispatch.confirm_seal,
        dns = dispatch.dns,
        documentation = dispatch.documentation,
        journey_id = dispatch.journey_id,
        logistician_status = dispatch.logistician_status,
        starting_fuel = dispatch.starting_fuel,
        starting_mileage = dispatch.starting_mileage,
        time_of_departure = dispatch.time_of_departure
    )

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

    // Basic CRUD operations
    fun getAllDispatches(): Flow<List<DispatchEntity>> = dispatchDao.getAllDispatches()

    suspend fun getDispatchById(id: Int): DispatchEntity? = withContext(Dispatchers.IO) {
        dispatchDao.getDispatchById(id)
    }

    suspend fun updateDispatch(dispatch: DispatchEntity) = withContext(Dispatchers.IO) {
        dispatchDao.updateDispatch(dispatch)
    }

    suspend fun deleteDispatch(dispatch: DispatchEntity) = withContext(Dispatchers.IO) {
        dispatchDao.deleteDispatch(dispatch)
    }
}