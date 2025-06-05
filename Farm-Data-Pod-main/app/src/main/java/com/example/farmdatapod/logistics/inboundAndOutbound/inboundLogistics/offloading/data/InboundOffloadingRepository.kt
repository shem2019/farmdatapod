package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.model.InboundOffloadingModel
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

class InboundOffloadingRepository(private val context: Context) : SyncableRepository {
    private val TAG = "InboundOffloadingRepository"
    private val offloadingDao = AppDatabase.getInstance(context).inboundOffloadingDao()

    suspend fun saveOffloading(entity: InboundOffloadingEntity, isOnline: Boolean): Result<InboundOffloadingEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to save offloading: ${entity.truck_offloading_number}, Online mode: $isOnline")

                // Check if the offloading already exists
                val existingOffloading = offloadingDao.getInboundOffloadingByNumber(entity.truck_offloading_number)
                val savedEntity: InboundOffloadingEntity

                if (existingOffloading == null) {
                    // Room returns Long for insert, convert it properly
                    val rowId = offloadingDao.insert(entity)
                    savedEntity = entity.copy(id = rowId.toInt())
                    Log.d(TAG, "Offloading saved locally with ID: ${rowId.toInt()}")
                } else {
                    savedEntity = existingOffloading.copy(
                        authorised = entity.authorised,
                        comment = entity.comment,
                        dispatcher = entity.dispatcher,
                        grns = entity.grns,
                        logistician_status = entity.logistician_status,
                        seal_number = entity.seal_number,
                        total_weight = entity.total_weight,
                        syncStatus = false,
                        lastModified = System.currentTimeMillis()
                    )
                    offloadingDao.update(savedEntity)
                    Log.d(TAG, "Offloading updated locally with ID: ${savedEntity.id}")
                }

                if (isOnline) {
                    try {
                        Log.d(TAG, "Attempting to sync with server...")
                        val model = savedEntity.toModel()

                        context?.let { ctx ->
                            RestClient.getApiService(ctx).offloading(model)
                                .enqueue(object : Callback<InboundOffloadingModel> {
                                    override fun onResponse(
                                        call: Call<InboundOffloadingModel>,
                                        response: Response<InboundOffloadingModel>
                                    ) {
                                        if (response.isSuccessful) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                savedEntity.id?.let { offloadingDao.markAsSynced(it) }
                                                Log.d(TAG, "Offloading synced with server")
                                            }
                                        } else {
                                            Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                        }
                                    }

                                    override fun onFailure(call: Call<InboundOffloadingModel>, t: Throwable) {
                                        Log.e(TAG, "Network error during sync", t)
                                    }
                                })
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during server sync", e)
                    }
                }
                Result.success(savedEntity)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving offloading", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedOffloadings = offloadingDao.getUnsyncedInboundOffloadings().first()
            Log.d(TAG, "Found ${unsyncedOffloadings.size} unsynced offloadings")

            var successCount = 0
            var failureCount = 0

            unsyncedOffloadings.forEach { entity ->
                try {
                    val model = entity.toModel()
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).offloading(model)
                            .enqueue(object : Callback<InboundOffloadingModel> {
                                override fun onResponse(
                                    call: Call<InboundOffloadingModel>,
                                    response: Response<InboundOffloadingModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            entity.id?.let { offloadingDao.markAsSynced(it) }
                                            successCount++
                                            Log.d(TAG, "Successfully synced ${entity.truck_offloading_number}")
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync ${entity.truck_offloading_number}")
                                    }
                                }

                                override fun onFailure(call: Call<InboundOffloadingModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing ${entity.truck_offloading_number}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${entity.truck_offloading_number}", e)
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
                context?.let { ctx ->
                    RestClient.getApiService(ctx).getOffloadings()
                        .enqueue(object : Callback<List<InboundOffloadingModel>> {
                            override fun onResponse(
                                call: Call<List<InboundOffloadingModel>>,
                                response: Response<List<InboundOffloadingModel>>
                            ) {
                                if (response.isSuccessful) {
                                    try {
                                        val serverOffloadings = response.body()
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                serverOffloadings?.forEach { model ->
                                                    val entity = model.toEntity()
                                                    val existingOffloading = offloadingDao.getInboundOffloadingByNumber(entity.truck_offloading_number)

                                                    if (existingOffloading == null) {
                                                        offloadingDao.insert(entity)
                                                        savedCount++
                                                    } else if (shouldUpdate(existingOffloading, entity)) {
                                                        offloadingDao.update(entity.copy(
                                                            id = existingOffloading.id,
                                                            syncStatus = true,
                                                            lastSynced = System.currentTimeMillis()
                                                        ))
                                                        savedCount++
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
                                    continuation.resume(Result.failure(RuntimeException("Unexpected response: ${response.code()}")))
                                }
                            }

                            override fun onFailure(call: Call<List<InboundOffloadingModel>>, t: Throwable) {
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

    private fun shouldUpdate(existing: InboundOffloadingEntity, new: InboundOffloadingEntity): Boolean {
        return existing.authorised != new.authorised ||
                existing.comment != new.comment ||
                existing.dispatcher != new.dispatcher ||
                existing.grns != new.grns ||
                existing.logistician_status != new.logistician_status ||
                existing.seal_number != new.seal_number ||
                existing.total_weight != new.total_weight
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

    fun getAllOffloadings(): Flow<List<InboundOffloadingEntity>> = offloadingDao.getAllInboundOffloadings()

    suspend fun getOffloadingById(id: Int): InboundOffloadingEntity? = withContext(Dispatchers.IO) {
        offloadingDao.getInboundOffloadingById(id)
    }

    suspend fun deleteOffloading(entity: InboundOffloadingEntity): InboundOffloadingEntity = withContext(Dispatchers.IO) {
        offloadingDao.delete(entity)
        entity
    }

    suspend fun updateOffloading(entity: InboundOffloadingEntity): InboundOffloadingEntity = withContext(Dispatchers.IO) {
        offloadingDao.update(entity)
        entity
    }
}

// Extension functions for converting between Entity and Model
private fun InboundOffloadingEntity.toModel() = InboundOffloadingModel(
    authorised = authorised,
    comment = comment,
    dispatcher = dispatcher,
    grns = grns,
    logistician_status = logistician_status,
    seal_number = seal_number,
    total_weight = total_weight,
    truck_offloading_number = truck_offloading_number
)

private fun InboundOffloadingModel.toEntity() = InboundOffloadingEntity(
    authorised = authorised,
    comment = comment,
    dispatcher = dispatcher,
    grns = grns,
    logistician_status = logistician_status,
    seal_number = seal_number,
    total_weight = total_weight,
    truck_offloading_number = truck_offloading_number,
    syncStatus = true,
    lastModified = System.currentTimeMillis(),
    lastSynced = System.currentTimeMillis()
)