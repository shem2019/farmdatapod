package com.example.farmdatapod.season.harvest.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.Buyer
import com.example.farmdatapod.models.PlanHarvestingModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.farmdatapod.sync.SyncStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

class HarvestPlanningRepository(private val context: Context) : SyncableRepository {
    private val TAG = "HarvestPlanningRepository"
    private val harvestPlanningDao = AppDatabase.getInstance(context).harvestPlanningDao()

    suspend fun saveHarvestPlan(
        harvestPlanning: HarvestPlanning,
        buyers: List<HarvestPlanningBuyer>,
        isOnline: Boolean
    ): Result<HarvestPlanning> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save harvest plan: ${harvestPlanning.field}, Online mode: $isOnline")

            // Save harvest planning and buyers in a transaction
            val localId = harvestPlanningDao.insertHarvestPlanningWithBuyers(harvestPlanning, buyers)
            Log.d(TAG, "Harvest plan saved locally with ID: $localId")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val planHarvestingModel = createPlanHarvestingModel(harvestPlanning, buyers)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).planHarvesting(planHarvestingModel)
                            .enqueue(object : Callback<PlanHarvestingModel> {
                                override fun onResponse(
                                    call: Call<PlanHarvestingModel>,
                                    response: Response<PlanHarvestingModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverModel ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                harvestPlanningDao.markHarvestPlanningAsSynced(localId)
                                                // Mark all related buyers as synced
                                                buyers.forEach { buyer ->
                                                    harvestPlanningDao.markBuyerAsSynced(buyer.id)
                                                }
                                                Log.d(TAG, "Harvest plan synced with server")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<PlanHarvestingModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Harvest plan will sync later")
            }
            Result.success(harvestPlanning)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving harvest plan", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedPlans = harvestPlanningDao.getUnsyncedHarvestPlannings()
            Log.d(TAG, "Found ${unsyncedPlans.size} unsynced harvest plans")

            var successCount = 0
            var failureCount = 0

            unsyncedPlans.forEach { plan ->
                try {
                    val buyers = harvestPlanningDao.getBuyersByHarvestPlanningId(plan.id)
                    val planHarvestingModel = createPlanHarvestingModel(plan, buyers)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).planHarvesting(planHarvestingModel)
                            .enqueue(object : Callback<PlanHarvestingModel> {
                                override fun onResponse(
                                    call: Call<PlanHarvestingModel>,
                                    response: Response<PlanHarvestingModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                harvestPlanningDao.markHarvestPlanningAsSynced(plan.id)  // Remove the .toInt()
                                                buyers.forEach { buyer ->
                                                    harvestPlanningDao.markBuyerAsSynced(buyer.id)
                                                }
                                                successCount++
                                                Log.d(TAG, "Successfully synced plan for ${plan.field}")
                                            }
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync plan for ${plan.field}")
                                    }
                                }

                                override fun onFailure(call: Call<PlanHarvestingModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing plan for ${plan.field}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing plan for ${plan.field}", e)
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
            return@withContext suspendCancellableCoroutine { continuation ->
                context?.let { ctx ->
                    RestClient.getApiService(ctx).getHarvestingPlans()
                        .enqueue(object : Callback<List<PlanHarvestingModel>> {
                            override fun onResponse(
                                call: Call<List<PlanHarvestingModel>>,
                                response: Response<List<PlanHarvestingModel>>
                            ) {
                                if (response.isSuccessful) {
                                    try {
                                        val serverPlans = response.body()

                                        CoroutineScope(Dispatchers.IO).launch {
                                            var savedCount = 0

                                            serverPlans?.forEach { serverPlan ->
                                                val localPlan = createLocalHarvestPlanFromServer(serverPlan)
                                                val localPlanId = harvestPlanningDao.insertHarvestPlanning(localPlan)  // Get the ID
                                                val buyers = createLocalBuyersFromServer(serverPlan, localPlanId)  // Pass the ID
                                                harvestPlanningDao.insertBuyers(buyers)  // Insert buyers separately
                                                savedCount++
                                            }

                                            continuation.resume(Result.success(savedCount))
                                        }
                                    } catch (e: Exception) {
                                        continuation.resume(Result.failure(e))
                                    }
                                } else {
                                    when (response.code()) {
                                        401, 403 -> continuation.resume(Result.failure(SecurityException("Authentication error")))
                                        else -> continuation.resume(Result.failure(RuntimeException("Server error")))
                                    }
                                }
                            }

                            override fun onFailure(call: Call<List<PlanHarvestingModel>>, t: Throwable) {
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

    private fun createPlanHarvestingModel(
        harvestPlanning: HarvestPlanning,
        buyers: List<HarvestPlanningBuyer>
    ): PlanHarvestingModel {
        return PlanHarvestingModel(
            field = harvestPlanning.field,
            season = harvestPlanning.season,
            producer = harvestPlanning.producer,
            date = harvestPlanning.date.toString(),
            labor_man_days = harvestPlanning.laborManDays,
            unit_cost_of_labor = harvestPlanning.unitCostOfLabor,
            start_time = harvestPlanning.startTime,
            end_time = harvestPlanning.endTime,
            weight_per_unit = harvestPlanning.weightPerUnit,
            price_per_unit = harvestPlanning.pricePerUnit,
            harvested_units = harvestPlanning.harvestedUnits,
            number_of_units = harvestPlanning.numberOfUnits,
            harvested_quality = harvestPlanning.harvestedQuality,
            comments = harvestPlanning.comments ?: "",  // Provide empty string as default
            season_planning_id = harvestPlanning.seasonPlanningId,
            buyers = buyers.map { buyer ->
                Buyer(
                    name = buyer.name,
                    contact_info = buyer.contactInfo,
                    quantity = buyer.quantity
                )
            }
        )
    }

    private fun createLocalHarvestPlanFromServer(serverPlan: PlanHarvestingModel): HarvestPlanning {
        return HarvestPlanning(
            field = serverPlan.field,
            season = serverPlan.season,
            producer = serverPlan.producer,
            date = serverPlan.date,
            laborManDays = serverPlan.labor_man_days,
            unitCostOfLabor = serverPlan.unit_cost_of_labor,
            startTime = serverPlan.start_time,
            endTime = serverPlan.end_time,
            weightPerUnit = serverPlan.weight_per_unit,
            pricePerUnit = serverPlan.price_per_unit,
            harvestedUnits = serverPlan.harvested_units,
            numberOfUnits = serverPlan.number_of_units,
            harvestedQuality = serverPlan.harvested_quality,
            comments = serverPlan.comments,
            seasonPlanningId = serverPlan.season_planning_id,
            isSynced = true
        )
    }

    private fun createLocalBuyersFromServer(serverPlan: PlanHarvestingModel, harvestPlanningId: Long): List<HarvestPlanningBuyer> {
        return serverPlan.buyers.map { buyerModel ->
            HarvestPlanningBuyer(
                name = buyerModel.name,
                contactInfo = buyerModel.contact_info,
                quantity = buyerModel.quantity,
                harvestPlanningId = harvestPlanningId,  // Added this
                isSynced = true
            )
        }
    }

    fun getAllHarvestPlans(): Flow<List<HarvestPlanningWithBuyers>> =
        harvestPlanningDao.getAllHarvestPlanningsWithBuyers()

    suspend fun getHarvestPlanById(id: Long): HarvestPlanningWithBuyers? = withContext(Dispatchers.IO) {
        harvestPlanningDao.getHarvestPlanningWithBuyers(id)
    }

    suspend fun deleteHarvestPlan(id: Long) = withContext(Dispatchers.IO) {
        harvestPlanningDao.deleteHarvestPlanningWithBuyers(id)
    }

    suspend fun updateHarvestPlan(
        harvestPlanning: HarvestPlanning,
        buyers: List<HarvestPlanningBuyer>
    ) = withContext(Dispatchers.IO) {
        harvestPlanningDao.updateHarvestPlanningWithBuyers(harvestPlanning, buyers)
    }
}