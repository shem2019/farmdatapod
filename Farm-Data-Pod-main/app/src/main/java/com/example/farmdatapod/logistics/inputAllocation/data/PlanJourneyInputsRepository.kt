package com.example.farmdatapod.logistics.inputAllocation.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.InputAllocationModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlanJourneyInputsRepository(private val context: Context) : SyncableRepository {
    private val TAG = "PlanJourneyInputsRepository"
    private val planJourneyInputsDao = AppDatabase.getInstance(context).planJourneyInputsDao()

    suspend fun savePlanJourneyInput(
        planJourneyInput: PlanJourneyInputsEntity,
        isOnline: Boolean
    ): Result<PlanJourneyInputsEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save plan journey input: $planJourneyInput, Online mode: $isOnline")

            // Check if the plan journey input already exists based on specific fields
            val existingPlanJourneyInput = planJourneyInputsDao.getPlanJourneyInputByFields(
                planJourneyInput.journey_id,
                planJourneyInput.stop_point_id,
                planJourneyInput.input,
                planJourneyInput.dn_number
            )

            val localId: Int
            if (existingPlanJourneyInput == null) {
                localId = planJourneyInputsDao.insert(planJourneyInput).toInt()
                Log.d(TAG, "Plan journey input saved locally with ID: $localId")
            } else {
                localId = existingPlanJourneyInput.id
                Log.d(TAG, "Plan journey input already exists locally with ID: $localId")
                updatePlanJourneyInputAndMarkForSync(existingPlanJourneyInput)
            }

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val inputAllocationModel = createInputAllocationModel(planJourneyInput)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).planJourneyInputs(inputAllocationModel)
                            .enqueue(object : Callback<InputAllocationModel> {
                                override fun onResponse(
                                    call: Call<InputAllocationModel>,
                                    response: Response<InputAllocationModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { _ ->
                                            // Switch to a coroutine context to call markAsSynced
                                            CoroutineScope(Dispatchers.IO).launch {
                                                planJourneyInputsDao.markAsSynced(localId)
                                                Log.d(TAG, "Plan journey input synced with server")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<InputAllocationModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Plan journey input will sync later")
            }
            Result.success(planJourneyInput)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving plan journey input", e)
            Result.failure(e)
        }
    }

    suspend fun updatePlanJourneyInputAndMarkForSync(planJourneyInput: PlanJourneyInputsEntity) {
        updatePlanJourneyInput(planJourneyInput)
        markForSync(planJourneyInput.id)
    }

    suspend fun markForSync(localId: Int) = withContext(Dispatchers.IO) {
        planJourneyInputsDao.markForSync(localId)
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedPlanJourneyInputs = planJourneyInputsDao.getUnsyncedPlanJourneyInputs()
            Log.d(TAG, "Found ${unsyncedPlanJourneyInputs.size} unsynced plan journey inputs")

            var successCount = 0
            var failureCount = 0

            unsyncedPlanJourneyInputs.forEach { planJourneyInput ->
                try {
                    val inputAllocationModel = createInputAllocationModel(planJourneyInput)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).planJourneyInputs(inputAllocationModel)
                            .enqueue(object : Callback<InputAllocationModel> {
                                override fun onResponse(
                                    call: Call<InputAllocationModel>,
                                    response: Response<InputAllocationModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { _ ->
                                            // Switch to a coroutine context to call markAsSynced
                                            CoroutineScope(Dispatchers.IO).launch {
                                                planJourneyInputsDao.markAsSynced(planJourneyInput.id)
                                                successCount++
                                                Log.d(TAG, "Successfully synced plan journey input with ID: ${planJourneyInput.id}")
                                            }
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync plan journey input with ID: ${planJourneyInput.id}")
                                    }
                                }

                                override fun onFailure(call: Call<InputAllocationModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing plan journey input with ID: ${planJourneyInput.id}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing plan journey input with ID: ${planJourneyInput.id}", e)
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
                RestClient.getApiService(ctx).getJourneyInputs()
                    .execute()
            }

            if (response?.isSuccessful == true) {
                response.body()?.let { journeyInputs ->
                    // Insert or update local database with server data
                    journeyInputs.forEach { inputAllocationModel ->
                        val existingInput = planJourneyInputsDao.getPlanJourneyInputByFields(
                            inputAllocationModel.journey_id.toString(),
                            inputAllocationModel.stop_point_id.toString(),
                            inputAllocationModel.input ?: "",
                            inputAllocationModel.dn_number ?: ""
                        )

                        if (existingInput == null) {
                            // Insert new record
                            val newEntity = createPlanJourneyInputsEntity(inputAllocationModel)
                            planJourneyInputsDao.insert(newEntity)
                        } else {
                            // Update existing record
                            val updatedEntity = existingInput.copy(
                                input = inputAllocationModel.input ?: existingInput.input,
                                number_of_units = inputAllocationModel.number_of_units ?: existingInput.number_of_units,
                                unit_cost = inputAllocationModel.unit_cost?.toDouble() ?: existingInput.unit_cost.toDouble(),
                                description = inputAllocationModel.description ?: existingInput.description,
                                syncStatus = true,
                                lastSynced = System.currentTimeMillis()
                            )
                            planJourneyInputsDao.update(updatedEntity)
                        }
                    }
                    Result.success(journeyInputs.size)
                } ?: Result.success(0)
            } else {
                Log.e(TAG, "Failed to fetch journey inputs from server")
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

    private fun createInputAllocationModel(planJourneyInput: PlanJourneyInputsEntity): InputAllocationModel {
        return InputAllocationModel(
            journey_id = planJourneyInput.journey_id.toInt(),
            stop_point_id = planJourneyInput.stop_point_id.toInt(),
            input = planJourneyInput.input,
            number_of_units = planJourneyInput.number_of_units,
            unit_cost = planJourneyInput.unit_cost.toInt(),
            description = planJourneyInput.description,
            dn_number = planJourneyInput.dn_number
        )
    }

    private fun createPlanJourneyInputsEntity(inputAllocationModel: InputAllocationModel): PlanJourneyInputsEntity {
        return PlanJourneyInputsEntity(
            server_id = 0, // You might want to use the server-generated ID if available
            journey_id = inputAllocationModel.journey_id.toString(),
            stop_point_id = inputAllocationModel.stop_point_id.toString(),
            input = inputAllocationModel.input ?: "",
            number_of_units = inputAllocationModel.number_of_units ?: 0,
            unit_cost = inputAllocationModel.unit_cost?.toDouble() ?: 0.0,
            description = inputAllocationModel.description ?: "",
            dn_number = inputAllocationModel.dn_number ?: "",
            syncStatus = true,
            lastSynced = System.currentTimeMillis()
        )
    }

    fun getAllPlanJourneyInputs(): Flow<List<PlanJourneyInputsEntity>> =
        planJourneyInputsDao.getAllPlanJourneyInputs()

    suspend fun getPlanJourneyInputById(id: Int): PlanJourneyInputsEntity? =
        withContext(Dispatchers.IO) {
            planJourneyInputsDao.getPlanJourneyInputById(id)
        }

    suspend fun deletePlanJourneyInput(planJourneyInput: PlanJourneyInputsEntity) =
        withContext(Dispatchers.IO) {
            planJourneyInputsDao.delete(planJourneyInput)
        }

    suspend fun updatePlanJourneyInput(planJourneyInput: PlanJourneyInputsEntity) =
        withContext(Dispatchers.IO) {
            planJourneyInputsDao.update(planJourneyInput)
        }
}