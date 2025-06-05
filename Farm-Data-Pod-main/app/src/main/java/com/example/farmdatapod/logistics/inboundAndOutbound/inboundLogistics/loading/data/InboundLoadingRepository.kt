package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.model.InboundLoadingModel
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

class InboundLoadingRepository(private val context: Context) : SyncableRepository {
    private val TAG = "InboundLoadingRepository"
    private val loadingDao = AppDatabase.getInstance(context).inboundLoadingDao()


    suspend fun saveLoading(entity: InboundLoadingEntity, isOnline: Boolean): Result<InboundLoadingEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to save loading: ${entity.truck_loading_number}, Online mode: $isOnline")

                // Check if the loading already exists
                val existingLoading = loadingDao.getInboundLoadingByNumber(entity.truck_loading_number)
                val savedEntity: InboundLoadingEntity

                if (existingLoading == null) {
                    val insertId = loadingDao.insert(entity).toInt()
                    savedEntity = entity.copy(id = insertId)
                    Log.d(TAG, "Loading saved locally with ID: $insertId")
                } else {
                    savedEntity = existingLoading.copy(
                        authorised = entity.authorised,
                        comment = entity.comment,
                        dispatcher = entity.dispatcher,
                        from_ = entity.from_,
                        grn = entity.grn,
                        logistician_status = entity.logistician_status,
                        seal_number = entity.seal_number,
                        to = entity.to,
                        total_weight = entity.total_weight,
                        syncStatus = false,
                        lastModified = System.currentTimeMillis()
                    )
                    loadingDao.update(savedEntity)
                    Log.d(TAG, "Loading updated locally with ID: ${savedEntity.id}")
                }

                if (isOnline) {
                    try {
                        Log.d(TAG, "Attempting to sync with server...")
                        val model = savedEntity.toModel()

                        context?.let { ctx ->
                            RestClient.getApiService(ctx).loading(model)
                                .enqueue(object : Callback<InboundLoadingModel> {
                                    override fun onResponse(
                                        call: Call<InboundLoadingModel>,
                                        response: Response<InboundLoadingModel>
                                    ) {
                                        if (response.isSuccessful) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                savedEntity.id?.let { loadingDao.markAsSynced(it) }
                                                Log.d(TAG, "Loading synced with server")
                                            }
                                        } else {
                                            Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                        }
                                    }

                                    override fun onFailure(call: Call<InboundLoadingModel>, t: Throwable) {
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
                Log.e(TAG, "Error saving loading", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedLoadings = loadingDao.getUnsyncedInboundLoadings().first()
            Log.d(TAG, "Found ${unsyncedLoadings.size} unsynced loadings")

            var successCount = 0
            var failureCount = 0

            unsyncedLoadings.forEach { entity ->
                try {
                    val model = entity.toModel()
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).loading(model)
                            .enqueue(object : Callback<InboundLoadingModel> {
                                override fun onResponse(
                                    call: Call<InboundLoadingModel>,
                                    response: Response<InboundLoadingModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            entity.id?.let { loadingDao.markAsSynced(it) }
                                            successCount++
                                            Log.d(TAG, "Successfully synced ${entity.truck_loading_number}")
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync ${entity.truck_loading_number}")
                                    }
                                }

                                override fun onFailure(call: Call<InboundLoadingModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing ${entity.truck_loading_number}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${entity.truck_loading_number}", e)
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
                    RestClient.getApiService(ctx).getLoadings()
                        .enqueue(object : Callback<List<InboundLoadingModel>> {
                            override fun onResponse(
                                call: Call<List<InboundLoadingModel>>,
                                response: Response<List<InboundLoadingModel>>
                            ) {
                                if (response.isSuccessful) {
                                    try {
                                        val serverLoadings = response.body()
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                serverLoadings?.forEach { model ->
                                                    val entity = model.toEntity()
                                                    val existingLoading = loadingDao.getInboundLoadingByNumber(entity.truck_loading_number)

                                                    if (existingLoading == null) {
                                                        loadingDao.insert(entity)
                                                        savedCount++
                                                    } else if (shouldUpdate(existingLoading, entity)) {
                                                        loadingDao.update(entity.copy(
                                                            id = existingLoading.id,
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

                            override fun onFailure(call: Call<List<InboundLoadingModel>>, t: Throwable) {
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

    private fun shouldUpdate(existing: InboundLoadingEntity, new: InboundLoadingEntity): Boolean {
        return existing.authorised != new.authorised ||
                existing.comment != new.comment ||
                existing.dispatcher != new.dispatcher ||
                existing.from_ != new.from_ ||
                existing.grn != new.grn ||
                existing.logistician_status != new.logistician_status ||
                existing.seal_number != new.seal_number ||
                existing.to != new.to ||
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

    fun getAllLoadings(): Flow<List<InboundLoadingEntity>> = loadingDao.getAllInboundLoadings()

    suspend fun getLoadingById(id: Int): InboundLoadingEntity? = withContext(Dispatchers.IO) {
        loadingDao.getInboundLoadingById(id)
    }

    suspend fun deleteLoading(entity: InboundLoadingEntity) = withContext(Dispatchers.IO) {
        loadingDao.delete(entity)
    }

    suspend fun updateLoading(entity: InboundLoadingEntity) = withContext(Dispatchers.IO) {
        loadingDao.update(entity)
    }
}

// Extension functions for converting between Entity and Model
private fun InboundLoadingEntity.toModel() = InboundLoadingModel(
    authorised = authorised,
    comment = comment,
    dispatcher = dispatcher,
    from_ = from_,
    grn = grn,
    logistician_status = logistician_status,
    seal_number = seal_number,
    to = to,
    total_weight = total_weight,
    truck_loading_number = truck_loading_number
)

private fun InboundLoadingModel.toEntity() = InboundLoadingEntity(
    authorised = authorised,
    comment = comment,
    dispatcher = dispatcher,
    from_ = from_,
    grn = grn,
    logistician_status = logistician_status,
    seal_number = seal_number,
    to = to,
    total_weight = total_weight,
    truck_loading_number = truck_loading_number,
    syncStatus = true,
    lastModified = System.currentTimeMillis(),
    lastSynced = System.currentTimeMillis()
)