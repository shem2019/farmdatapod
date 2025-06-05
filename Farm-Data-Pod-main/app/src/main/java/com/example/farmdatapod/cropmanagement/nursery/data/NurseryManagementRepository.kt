package com.example.farmdatapod.cropmanagement.nursery.data

import com.example.farmdatapod.season.nursery.data.InputEntity
import com.example.farmdatapod.season.nursery.data.ManagementActivityEntity
import com.example.farmdatapod.season.nursery.data.NurseryPlanEntity
import com.example.farmdatapod.season.nursery.data.NurseryPlanWithRelations


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.Input
import com.example.farmdatapod.models.NurseryManagementActivity
import com.example.farmdatapod.models.PlanNurseryModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

class NurseryManagementRepository(private val context: Context) : SyncableRepository {
    private val TAG = "NurseryManagementRepository"
    private val nurseryPlanDao = AppDatabase.getInstance(context).nurseryPlanDao()

    suspend fun saveNurseryPlan(
        nurseryPlan: NurseryPlanEntity,
        activities: List<Pair<ManagementActivityEntity, List<InputEntity>>>,
        isOnline: Boolean
    ): Result<NurseryPlanEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save nursery plan: ${nurseryPlan.crop}, Online mode: $isOnline")

            // Insert and get the ID
            val localId = nurseryPlanDao.insertFullNurseryPlan(nurseryPlan, activities)
            Log.d(TAG, "Nursery plan saved locally with ID: $localId")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val planRequest = createPlanRequest(nurseryPlan, activities)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).nurseryManagement(planRequest)
                            .enqueue(object : Callback<PlanNurseryModel> {
                                override fun onResponse(
                                    call: Call<PlanNurseryModel>,
                                    response: Response<PlanNurseryModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverPlan ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                nurseryPlanDao.markAsUploaded(localId)
                                                Log.d(TAG, "Nursery plan synced with server")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<PlanNurseryModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            }
            Result.success(nurseryPlan)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving nursery plan", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedPlans = nurseryPlanDao.getUnsyncedPlans()
            Log.d(TAG, "Found ${unsyncedPlans.size} unsynced nursery plans")

            var successCount = 0
            var failureCount = 0

            unsyncedPlans.forEach { planWithRelations ->
                try {
                    val planRequest = createPlanRequest(
                        planWithRelations.nurseryPlan,
                        planWithRelations.managementActivities.map { activityWithInputs ->
                            Pair(activityWithInputs.activity, activityWithInputs.inputs)
                        }
                    )

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).nurseryManagement(planRequest)
                            .enqueue(object : Callback<PlanNurseryModel> {
                                override fun onResponse(
                                    call: Call<PlanNurseryModel>,
                                    response: Response<PlanNurseryModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            nurseryPlanDao.markAsUploaded(planWithRelations.nurseryPlan.id)
                                            successCount++
                                            Log.d(TAG, "Successfully synced plan for ${planWithRelations.nurseryPlan.crop}")
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync plan for ${planWithRelations.nurseryPlan.crop}")
                                    }
                                }

                                override fun onFailure(call: Call<PlanNurseryModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing plan for ${planWithRelations.nurseryPlan.crop}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing plan for ${planWithRelations.nurseryPlan.crop}", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    private fun createPlanRequest(
        plan: NurseryPlanEntity,
        activities: List<Pair<ManagementActivityEntity, List<InputEntity>>>
    ): PlanNurseryModel {
        return PlanNurseryModel(
            producer = plan.producer,
            season = plan.season,
            date_of_establishment = plan.dateOfEstablishment,
            crop_cycle_weeks = plan.cropCycleWeeks,
            crop = plan.crop,
            variety = plan.variety,
            seed_batch_number = plan.seedBatchNumber,
            type_of_trays = plan.typeOfTrays,
            number_of_trays = plan.numberOfTrays,
            comments = plan.comments ?: "",
            season_planning_id = plan.seasonPlanningId.toInt(),
            nursery_management_activity = activities.map { (activity, inputs) ->
                NurseryManagementActivity(
                    management_activity = activity.managementActivity,
                    frequency = activity.frequency,
                    man_days = activity.manDays,
                    unit_cost_of_labor = activity.unitCostOfLabor,
                    input = inputs.map { input ->
                        Input(
                            input = input.input,
                            input_cost = input.inputCost
                        )
                    }
                )
            }
        )
    }

    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var savedCount = 0

            return@withContext suspendCancellableCoroutine { continuation ->
                context?.let { ctx ->
                    RestClient.getApiService(ctx).getNurseryManagement()
                        .enqueue(object : Callback<List<PlanNurseryModel>> {
                            override fun onResponse(
                                call: Call<List<PlanNurseryModel>>,
                                response: Response<List<PlanNurseryModel>>
                            ) {
                                if (response.isSuccessful) {
                                    try {
                                        val serverPlans = response.body()
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                serverPlans?.forEach { serverPlan ->
                                                    val nurseryPlan = NurseryPlanEntity(
                                                        producer = serverPlan.producer,
                                                        season = serverPlan.season,
                                                        dateOfEstablishment = serverPlan.date_of_establishment,
                                                        cropCycleWeeks = serverPlan.crop_cycle_weeks,
                                                        crop = serverPlan.crop,
                                                        variety = serverPlan.variety,
                                                        seedBatchNumber = serverPlan.seed_batch_number,
                                                        typeOfTrays = serverPlan.type_of_trays,
                                                        numberOfTrays = serverPlan.number_of_trays,
                                                        comments = serverPlan.comments,
                                                        seasonPlanningId = serverPlan.season_planning_id.toLong(),
                                                        isUploaded = true
                                                    )

                                                    val activities = serverPlan.nursery_management_activity.map { activity ->
                                                        val managementActivity = ManagementActivityEntity(
                                                            managementActivity = activity.management_activity,
                                                            frequency = activity.frequency,
                                                            manDays = activity.man_days,
                                                            unitCostOfLabor = activity.unit_cost_of_labor,
                                                            nurseryPlanId = 0 // Will be set during insertion
                                                        )

                                                        val inputs = activity.input.map { input ->
                                                            InputEntity(
                                                                input = input.input,
                                                                inputCost = input.input_cost,
                                                                managementActivityId = 0 // Will be set during insertion
                                                            )
                                                        }

                                                        Pair(managementActivity, inputs)
                                                    }

                                                    nurseryPlanDao.insertFullNurseryPlan(nurseryPlan, activities)
                                                    savedCount++
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

                            override fun onFailure(call: Call<List<PlanNurseryModel>>, t: Throwable) {
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

    fun getAllNurseryPlans(): Flow<List<NurseryPlanWithRelations>> = nurseryPlanDao.getAllNurseryPlans()

    suspend fun getNurseryPlanById(id: Long): NurseryPlanWithRelations? = withContext(Dispatchers.IO) {
        nurseryPlanDao.getNurseryPlanById(id)
    }

    suspend fun deleteNurseryPlan(plan: NurseryPlanEntity) = withContext(Dispatchers.IO) {
        nurseryPlanDao.deleteNurseryPlan(plan)
    }
}