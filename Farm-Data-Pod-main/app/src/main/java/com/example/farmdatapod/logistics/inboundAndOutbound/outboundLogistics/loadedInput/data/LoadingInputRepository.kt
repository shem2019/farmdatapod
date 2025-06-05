package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.models.LoadingInputModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.await
import java.util.concurrent.TimeUnit

class LoadingInputRepository(context: Context) : SyncableRepository {
    private val TAG = "LoadingInputRepository"
    private val database = AppDatabase.getInstance(context)
    private val loadingInputDao = database.loadingInputDao()
    private val apiService = RestClient.getApiService(context)

    private var syncJob: Job? = null

    suspend fun saveLoadingInput(input: LoadingInputEntity, isOnline: Boolean): Result<LoadingInputEntity> =
        withContext(Dispatchers.IO) {
            try {
                database.withTransaction {
                    Log.d(TAG, "Attempting to save loading input: ${input.delivery_note_number}, Online mode: $isOnline")

                    val existingInput = loadingInputDao.getLoadingInputByDeliveryNote(input.delivery_note_number)
                    val localId = if (existingInput == null) {
                        loadingInputDao.insertLoadingInput(input)
                    } else {
                        updateInputAndMarkForSync(existingInput)
                        existingInput.id.toLong()
                    }

                    if (isOnline) {
                        try {
                            withTimeout(TimeUnit.SECONDS.toMillis(30)) {
                                val loadingInputModel = createLoadingInputModel(input)
                                val response = apiService.loadedInputs(loadingInputModel).await()

                                loadingInputDao.updateSyncStatus(
                                    localId.toInt(),
                                    true,
                                    System.currentTimeMillis()
                                )
                                Log.d(TAG, "Loading input synced successfully")
                            }
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Sync cancelled - this is normal during navigation")
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during server sync", e)
                            loadingInputDao.updateSyncStatus(localId.toInt(), false, null)
                        }
                    }

                    Result.success(input)
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Operation cancelled - this is normal during navigation")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error saving loading input", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                var successCount = 0
                var failureCount = 0

                val unsyncedInputs = loadingInputDao.getUnsyncedLoadingInputs().first()
                Log.d(TAG, "Found ${unsyncedInputs.size} unsynced loading inputs")

                unsyncedInputs.forEach { input ->
                    try {
                        withTimeout(TimeUnit.SECONDS.toMillis(30)) {
                            val loadingInputModel = createLoadingInputModel(input)
                            val response = apiService.loadedInputs(loadingInputModel).await()

                            loadingInputDao.updateSyncStatus(
                                input.id,
                                true,
                                System.currentTimeMillis()
                            )
                            successCount++
                            Log.d(TAG, "Successfully synced input ${input.delivery_note_number}")
                        }
                    } catch (e: CancellationException) {
                        Log.d(TAG, "Sync cancelled - this is normal during navigation")
                        throw e
                    } catch (e: Exception) {
                        failureCount++
                        Log.e(TAG, "Error syncing input ${input.delivery_note_number}", e)
                    }
                }

                Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Sync operation cancelled - this is normal during navigation")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val serverInputs = apiService.getLoadedInputs().await()
                var savedCount = 0

                serverInputs.forEach { serverInput ->
                    try {
                        processServerInput(serverInput)?.let { savedCount++ }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing input ${serverInput.delivery_note_number}", e)
                    }
                }

                Result.success(savedCount)
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Server sync cancelled - this is normal during navigation")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    private suspend fun processServerInput(serverInput: LoadingInputModel): LoadingInputEntity? {
        return database.withTransaction {
            val existingInput = loadingInputDao.getLoadingInputByDeliveryNote(serverInput.delivery_note_number)

            when {
                existingInput == null -> {
                    val localInput = createLocalInputFromServer(serverInput)
                    loadingInputDao.insertLoadingInput(localInput)
                    Log.d(TAG, "Inserted new input: ${serverInput.delivery_note_number}")
                    localInput
                }
                shouldUpdate(existingInput, serverInput) -> {
                    val updatedInput = createUpdatedInput(existingInput, serverInput)
                    loadingInputDao.updateLoadingInput(updatedInput)
                    Log.d(TAG, "Updated existing input: ${serverInput.delivery_note_number}")
                    updatedInput
                }
                else -> {
                    Log.d(TAG, "Input ${serverInput.delivery_note_number} is up to date")
                    null
                }
            }
        }
    }

    override suspend fun performFullSync(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
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
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Full sync cancelled - this is normal during navigation")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during full sync", e)
            Result.failure(e)
        }
    }

    private suspend fun updateInputAndMarkForSync(input: LoadingInputEntity) {
        database.withTransaction {
            loadingInputDao.updateLoadingInput(input)
            loadingInputDao.updateSyncStatus(input.id, false, null)
        }
    }

    // Helper methods for creating models and entities
    private fun createLoadingInputModel(input: LoadingInputEntity) = LoadingInputModel(
        authorised = input.authorised,
        delivery_note_number = input.delivery_note_number,
        input = input.input.toString(),
        journey_id = input.journey_id,
        quantity_loaded = input.quantity_loaded,
        stop_point_id = input.stop_point_id
    )

    private fun createLocalInputFromServer(serverInput: LoadingInputModel) = LoadingInputEntity(
        server_id = 0L,
        authorised = serverInput.authorised,
        delivery_note_number = serverInput.delivery_note_number,
        input = serverInput.input.toInt(),
        journey_id = serverInput.journey_id,
        quantity_loaded = serverInput.quantity_loaded,
        stop_point_id = serverInput.stop_point_id,
        syncStatus = true,
        lastSynced = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis()
    )

    private fun createUpdatedInput(existingInput: LoadingInputEntity, serverInput: LoadingInputModel) =
        existingInput.copy(
            authorised = serverInput.authorised,
            input = serverInput.input.toInt(),
            journey_id = serverInput.journey_id,
            quantity_loaded = serverInput.quantity_loaded,
            stop_point_id = serverInput.stop_point_id,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )

    private fun shouldUpdate(existingInput: LoadingInputEntity, serverInput: LoadingInputModel): Boolean =
        existingInput.authorised != serverInput.authorised ||
                existingInput.input != serverInput.input.toInt() ||
                existingInput.journey_id != serverInput.journey_id ||
                existingInput.quantity_loaded != serverInput.quantity_loaded ||
                existingInput.stop_point_id != serverInput.stop_point_id

    // Database query methods
    fun getAllLoadingInputs(): Flow<List<LoadingInputEntity>> = loadingInputDao.getAllLoadingInputs()

    fun getLoadingInputsByJourneyId(journeyId: Int): Flow<List<LoadingInputEntity>> =
        loadingInputDao.getLoadingInputsByJourneyId(journeyId)

    suspend fun deleteLoadingInput(input: LoadingInputEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            loadingInputDao.deleteLoadingInput(input)
        }
    }

    fun cleanup() {
        syncJob?.cancel()
        syncJob = null
    }

    companion object {
        private const val TAG = "LoadingInputRepository"
    }
}