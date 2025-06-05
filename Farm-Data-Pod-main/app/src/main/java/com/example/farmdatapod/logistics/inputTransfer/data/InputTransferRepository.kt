package com.example.farmdatapod.logistics.inputTransfer.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.logistics.inputTransfer.model.InputTransferModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InputTransferRepository(private val context: Context) : SyncableRepository {
    private val TAG = "InputTransferRepository"
    private val inputTransferDao = AppDatabase.getInstance(context).inputTransferDao()

    suspend fun saveInputTransfer(
        inputTransfer: InputTransferEntity,
        isOnline: Boolean
    ): Result<InputTransferEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save input transfer: $inputTransfer, Online mode: $isOnline")

            // Save locally first
            inputTransferDao.insertInputTransfer(inputTransfer)
            Log.d(TAG, "Input transfer saved locally")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val inputTransferModel = createInputTransferModel(inputTransfer)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).inputTransferRequest(inputTransferModel)
                            .enqueue(object : Callback<InputTransferModel> {
                                override fun onResponse(
                                    call: Call<InputTransferModel>,
                                    response: Response<InputTransferModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val updatedEntity = inputTransfer.copy(
                                                syncStatus = true,
                                                lastSynced = System.currentTimeMillis()
                                            )
                                            inputTransferDao.updateInputTransfer(updatedEntity)
                                            Log.d(TAG, "Input transfer synced with server")
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<InputTransferModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Input transfer will sync later")
            }
            Result.success(inputTransfer)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving input transfer", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedTransfers = inputTransferDao.getUnsyncedInputTransfers().first()
            Log.d(TAG, "Found ${unsyncedTransfers.size} unsynced input transfers")

            var successCount = 0
            var failureCount = 0

            unsyncedTransfers.forEach { transfer ->
                try {
                    val inputTransferModel = createInputTransferModel(transfer)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).inputTransferRequest(inputTransferModel)
                            .enqueue(object : Callback<InputTransferModel> {
                                override fun onResponse(
                                    call: Call<InputTransferModel>,
                                    response: Response<InputTransferModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            inputTransferDao.markAsSynced(transfer.id)
                                            successCount++
                                            Log.d(TAG, "Successfully synced input transfer with ID: ${transfer.id}")
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync input transfer with ID: ${transfer.id}")
                                    }
                                }

                                override fun onFailure(call: Call<InputTransferModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing input transfer with ID: ${transfer.id}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing input transfer with ID: ${transfer.id}", e)
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
            val response = context?.let { ctx ->
                RestClient.getApiService(ctx).getInputTransferRequests()
                    .execute()
            }

            if (response?.isSuccessful == true) {
                response.body()?.let { transfers ->
                    transfers.forEach { transferModel ->
                        val entity = createInputTransferEntity(transferModel)
                        inputTransferDao.insertInputTransfer(entity)
                    }
                    Result.success(transfers.size)
                } ?: Result.success(0)
            } else {
                Log.e(TAG, "Failed to fetch input transfers from server")
                Result.success(0)
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

    private fun createInputTransferModel(entity: InputTransferEntity): InputTransferModel {
        return InputTransferModel(
            destination_hub_id = entity.destination_hub_id,
            input = entity.input,
            origin_hub_id = entity.origin_hub_id,
            quantity = entity.quantity,
            status = entity.status
        )
    }

    private fun createInputTransferEntity(model: InputTransferModel): InputTransferEntity {
        return InputTransferEntity(
            server_id = 0, // Will be updated when synced
            destination_hub_id = model.destination_hub_id,
            input = model.input,
            origin_hub_id = model.origin_hub_id,
            quantity = model.quantity,
            status = model.status,
            syncStatus = true,
            lastSynced = System.currentTimeMillis()
        )
    }

    // Convenience methods for accessing data
    fun getAllInputTransfers(): Flow<List<InputTransferEntity>> =
        inputTransferDao.getAllInputTransfers()

    suspend fun getInputTransferById(id: Int): InputTransferEntity? =
        withContext(Dispatchers.IO) {
            inputTransferDao.getInputTransferById(id)
        }

    suspend fun getInputTransferByServerId(serverId: Long): InputTransferEntity? =
        withContext(Dispatchers.IO) {
            inputTransferDao.getInputTransferByServerId(serverId)
        }

    suspend fun deleteInputTransfer(inputTransfer: InputTransferEntity) =
        withContext(Dispatchers.IO) {
            inputTransferDao.deleteInputTransfer(inputTransfer)
        }

    suspend fun updateInputTransfer(inputTransfer: InputTransferEntity) =
        withContext(Dispatchers.IO) {
            inputTransferDao.updateInputTransfer(inputTransfer)
        }

    fun getInputTransfersByHub(hubId: Int): Flow<List<InputTransferEntity>> =
        inputTransferDao.getInputTransfersByHub(hubId)

    fun getUnsyncedCount(): Flow<Int> =
        inputTransferDao.getUnsyncedCount()
}