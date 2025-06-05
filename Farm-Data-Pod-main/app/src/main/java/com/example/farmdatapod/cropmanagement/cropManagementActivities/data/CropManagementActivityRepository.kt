package com.example.farmdatapod.cropmanagement.cropManagementActivities.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.CropManagementModel
import com.example.farmdatapod.models.GappingActivity
import com.example.farmdatapod.models.PruningActivity
import com.example.farmdatapod.models.StakingActivity
import com.example.farmdatapod.models.ThinningActivity
import com.example.farmdatapod.models.WateringActivity
import com.example.farmdatapod.models.WeedingActivity
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.season.cropManagement.data.CropManagementEntity
import com.example.farmdatapod.season.cropManagement.data.CropManagementWithActivities
import com.example.farmdatapod.season.cropManagement.data.GappingActivityEntity
import com.example.farmdatapod.season.cropManagement.data.PruningActivityEntity
import com.example.farmdatapod.season.cropManagement.data.StakingActivityEntity
import com.example.farmdatapod.season.cropManagement.data.ThinningActivityEntity
import com.example.farmdatapod.season.cropManagement.data.WateringActivityEntity
import com.example.farmdatapod.season.cropManagement.data.WeedingActivityEntity
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

class CropManagementActivityRepository(private val context: Context) : SyncableRepository {
    private val TAG = "CropManagementActivityRepository"
    private val cropManagementDao = AppDatabase.getInstance(context).cropManagementDao()

    suspend fun saveCropManagement(
        cropManagement: CropManagementModel,
        isOnline: Boolean
    ): Result<CropManagementModel> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save crop management activity, Online mode: $isOnline")

            val existingActivity = cropManagementDao.getCropManagementByFields(
                cropManagement.producer,
                cropManagement.season,
                cropManagement.field,
                cropManagement.date
            )

            val localId: Long = if (existingActivity == null) {
                val cropManagementEntity = CropManagementEntity(
                    producer = cropManagement.producer,
                    season = cropManagement.season,
                    field = cropManagement.field,
                    date = cropManagement.date,
                    seasonPlanningId = cropManagement.season_planning_id,
                    comments = cropManagement.comments,
                    syncStatus = false,
                    lastModified = System.currentTimeMillis(),
                    lastSynced = null
                )

                cropManagementDao.insertCropManagementWithActivities(
                    cropManagementEntity,
                    listOf(convertToGappingEntity(cropManagement.gappingActivity)),
                    listOf(convertToWeedingEntity(cropManagement.weedingActivity)),
                    listOf(convertToPruningEntity(cropManagement.pruningActivity)),
                    listOf(convertToStakingEntity(cropManagement.stakingActivity)),
                    listOf(convertToThinningEntity(cropManagement.thinningActivity)),
                    listOf(convertToWateringEntity(cropManagement.wateringActivity))
                )
            } else {
                existingActivity.id.toLong().also {
                    updateCropManagementAndMarkForSync(existingActivity)
                }
            }

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).cropManagementActivities(cropManagement)
                            .enqueue(object : Callback<CropManagementModel> {
                                override fun onResponse(
                                    call: Call<CropManagementModel>,
                                    response: Response<CropManagementModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverResponse ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                cropManagementDao.markAsSynced(
                                                    localId.toInt(),
                                                    serverResponse.id,
                                                    System.currentTimeMillis()
                                                )
                                                Log.d(
                                                    TAG,
                                                    "Crop management synced with server ID: ${serverResponse.id}"
                                                )
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(
                                    call: Call<CropManagementModel>,
                                    t: Throwable
                                ) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Will sync later")
            }

            Result.success(cropManagement)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving crop management", e)
            Result.failure(e)
        }
    }

    private suspend fun updateCropManagementAndMarkForSync(activity: CropManagementEntity) {
        activity.apply {
            lastModified = System.currentTimeMillis()
            syncStatus = false
        }
        cropManagementDao.updateCropManagement(activity)
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedActivities = cropManagementDao.getUnsyncedCropManagement()
            Log.d(TAG, "Found ${unsyncedActivities.size} unsynced crop management activities")

            var successCount = 0
            var failureCount = 0

            unsyncedActivities.forEach { activityWithRelations ->
                try {
                    val cropManagementModel = convertToModel(activityWithRelations)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).cropManagementActivities(cropManagementModel)
                            .enqueue(object : Callback<CropManagementModel> {
                                override fun onResponse(
                                    call: Call<CropManagementModel>,
                                    response: Response<CropManagementModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverResponse ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                cropManagementDao.markAsSynced(
                                                    activityWithRelations.cropManagement.id,
                                                    serverResponse.id?.toString(),
                                                    System.currentTimeMillis()
                                                )
                                                successCount++
                                                Log.d(
                                                    TAG,
                                                    "Successfully synced activity ID: ${activityWithRelations.cropManagement.id}"
                                                )
                                            }
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(
                                            TAG,
                                            "Failed to sync activity ID: ${activityWithRelations.cropManagement.id}"
                                        )
                                    }
                                }

                                override fun onFailure(
                                    call: Call<CropManagementModel>,
                                    t: Throwable
                                ) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing activity", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing activity", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            AppDatabase.getInstance(context).runInTransaction {
                launch {
                    try {
                        val duplicates = cropManagementDao.findDuplicateActivities()
                        if (duplicates.isNotEmpty()) {
                            val deletedCount = cropManagementDao.deleteDuplicateActivities()
                            Log.d(TAG, "Cleaned $deletedCount duplicate records")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cleaning duplicates", e)
                    }
                }
            }

            return@withContext suspendCancellableCoroutine { continuation ->
                context?.let { ctx ->
                    RestClient.getApiService(ctx).getCropManagement()
                        .enqueue(object : Callback<List<CropManagementModel>> {
                            override fun onResponse(
                                call: Call<List<CropManagementModel>>,
                                response: Response<List<CropManagementModel>>
                            ) {
                                if (response.isSuccessful) {
                                    response.body()?.let { activities ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            var savedCount = 0
                                            AppDatabase.getInstance(context).runInTransaction {
                                                activities.distinctBy { "${it.producer}:${it.season}:${it.field}:${it.date}" }
                                                    .forEach { serverActivity ->
                                                        try {
                                                            launch {
                                                                processServerActivity(serverActivity)?.let { savedCount++ }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(
                                                                TAG,
                                                                "Error processing activity",
                                                                e
                                                            )
                                                        }
                                                    }
                                            }
                                            continuation.resume(Result.success(savedCount))
                                        }
                                    }
                                } else {
                                    when (response.code()) {
                                        401, 403 -> continuation.resume(
                                            Result.failure(
                                                SecurityException("Authentication error: ${response.code()}")
                                            )
                                        )

                                        else -> continuation.resume(
                                            Result.failure(
                                                RuntimeException(
                                                    "Unexpected response: ${response.code()}"
                                                )
                                            )
                                        )
                                    }
                                }
                            }

                            override fun onFailure(
                                call: Call<List<CropManagementModel>>,
                                t: Throwable
                            ) {
                                continuation.resume(Result.failure(t))
                            }
                        })
                } ?: continuation.resume(Result.failure(RuntimeException("Context is null")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private suspend fun processServerActivity(serverActivity: CropManagementModel): CropManagementEntity? {
        val existingActivity = cropManagementDao.getCropManagementByFields(
            serverActivity.producer,
            serverActivity.season,
            serverActivity.field,
            serverActivity.date
        )

        return when {
            existingActivity == null -> {
                val localActivity = createLocalActivityFromServer(serverActivity)
                cropManagementDao.insertCropManagement(localActivity)
                Log.d(TAG, "Inserted new activity")
                localActivity
            }

            shouldUpdate(existingActivity, serverActivity) -> {
                val updatedActivity = createUpdatedActivity(existingActivity, serverActivity)
                cropManagementDao.updateCropManagement(updatedActivity)
                Log.d(TAG, "Updated existing activity")
                updatedActivity
            }

            else -> {
                Log.d(TAG, "Activity is up to date")
                null
            }
        }
    }

    private fun shouldUpdate(
        existingActivity: CropManagementEntity,
        serverActivity: CropManagementModel
    ): Boolean {
        return existingActivity.serverId != serverActivity.id ||
                existingActivity.producer != serverActivity.producer ||
                existingActivity.season != serverActivity.season ||
                existingActivity.field != serverActivity.field ||
                existingActivity.date != serverActivity.date ||
                existingActivity.seasonPlanningId != serverActivity.season_planning_id ||
                existingActivity.comments != serverActivity.comments
    }

    private fun createLocalActivityFromServer(serverActivity: CropManagementModel): CropManagementEntity {
        return CropManagementEntity(
            serverId = serverActivity.id,
            producer = serverActivity.producer,
            season = serverActivity.season,
            field = serverActivity.field,
            date = serverActivity.date,
            seasonPlanningId = serverActivity.season_planning_id,
            comments = serverActivity.comments,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun createUpdatedActivity(
        existingActivity: CropManagementEntity,
        serverActivity: CropManagementModel
    ): CropManagementEntity {
        return existingActivity.copy(
            serverId = serverActivity.id,
            producer = serverActivity.producer,
            season = serverActivity.season,
            field = serverActivity.field,
            date = serverActivity.date,
            seasonPlanningId = serverActivity.season_planning_id,
            comments = serverActivity.comments,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun convertToGappingEntity(activity: GappingActivity): GappingActivityEntity {
        return GappingActivityEntity(
            cropManagementId = 0, // This will be updated after insertion
            cropPopulation = activity.crop_population,
            manDays = activity.man_days,
            plantingMaterial = activity.planting_material,
            targetPopulation = activity.target_population,
            unitCostOfLabor = activity.unit_cost_of_labor
        )
    }

    private fun convertToWeedingEntity(activity: WeedingActivity): WeedingActivityEntity {
        return WeedingActivityEntity(
            cropManagementId = 0, // This will be updated after insertion
            input = activity.input,
            manDays = activity.man_days,
            methodOfWeeding = activity.method_of_weeding,
            unitCostOfLabor = activity.unit_cost_of_labor
        )
    }

    private fun convertToPruningEntity(activity: PruningActivity): PruningActivityEntity {
        return PruningActivityEntity(
            cropManagementId = 0, // This will be updated after insertion
            equipmentUsed = activity.equipment_used,
            manDays = activity.man_days,
            unitCostOfLabor = activity.unit_cost_of_labor
        )
    }

    private fun convertToStakingEntity(activity: StakingActivity): StakingActivityEntity {
        return StakingActivityEntity(
            cropManagementId = 0, // This will be updated after insertion
            costPerUnit = activity.cost_per_unit,
            manDays = activity.man_days,
            unitCostOfLabor = activity.unit_cost_of_labor,
            unitStakes = activity.unit_stakes
        )
    }

    private fun convertToThinningEntity(activity: ThinningActivity): ThinningActivityEntity {
        return ThinningActivityEntity(
            cropManagementId = 0, // This will be updated after insertion
            equipmentUsed = activity.equipment_used,
            manDays = activity.man_days,
            unitCostOfLabor = activity.unit_cost_of_labor
        )
    }

    private fun convertToWateringEntity(activity: WateringActivity): WateringActivityEntity {
        return WateringActivityEntity(
            cropManagementId = 0, // This will be updated after insertion
            costOfFuel = activity.cost_of_fuel,
            dischargeHours = activity.discharge_hours,
            endTime = activity.end_time,
            frequencyOfWatering = activity.frequency_of_watering,
            manDays = activity.man_days,
            startTime = activity.start_time,
            typeOfIrrigation = activity.type_of_irrigation,
            unitCost = activity.unit_cost,
            unitCostOfLabor = activity.unit_cost_of_labor
        )
    }

    private fun convertFromGappingEntity(entity: GappingActivityEntity): GappingActivity {
        return GappingActivity(
            activity = "Gapping",
            crop_population = entity.cropPopulation,
            man_days = entity.manDays,
            planting_material = entity.plantingMaterial,
            target_population = entity.targetPopulation,
            unit_cost_of_labor = entity.unitCostOfLabor
        )
    }

    private fun convertFromWeedingEntity(entity: WeedingActivityEntity): WeedingActivity {
        return WeedingActivity(
            activity = "Weeding",
            input = entity.input,
            man_days = entity.manDays,
            method_of_weeding = entity.methodOfWeeding,
            unit_cost_of_labor = entity.unitCostOfLabor
        )
    }

    private fun convertFromPruningEntity(entity: PruningActivityEntity): PruningActivity {
        return PruningActivity(
            activity = "Pruning",
            equipment_used = entity.equipmentUsed,
            man_days = entity.manDays,
            unit_cost_of_labor = entity.unitCostOfLabor
        )
    }

    private fun convertFromStakingEntity(entity: StakingActivityEntity): StakingActivity {
        return StakingActivity(
            activity = "Staking",
            cost_per_unit = entity.costPerUnit,
            man_days = entity.manDays,
            unit_cost_of_labor = entity.unitCostOfLabor,
            unit_stakes = entity.unitStakes
        )
    }

    private fun convertFromThinningEntity(entity: ThinningActivityEntity): ThinningActivity {
        return ThinningActivity(
            activity = "Thinning",
            equipment_used = entity.equipmentUsed,
            man_days = entity.manDays,
            unit_cost_of_labor = entity.unitCostOfLabor
        )
    }

    private fun convertFromWateringEntity(entity: WateringActivityEntity): WateringActivity {
        return WateringActivity(
            activity = "Watering",
            cost_of_fuel = entity.costOfFuel,
            discharge_hours = entity.dischargeHours,
            end_time = entity.endTime,
            frequency_of_watering = entity.frequencyOfWatering,
            man_days = entity.manDays,
            start_time = entity.startTime,
            type_of_irrigation = entity.typeOfIrrigation,
            unit_cost = entity.unitCost,
            unit_cost_of_labor = entity.unitCostOfLabor
        )
    }

    private fun convertToModel(entityWithRelations: CropManagementWithActivities): CropManagementModel {
        return CropManagementModel(
            id = entityWithRelations.cropManagement.serverId,
            producer = entityWithRelations.cropManagement.producer,
            season = entityWithRelations.cropManagement.season,
            field = entityWithRelations.cropManagement.field,
            date = entityWithRelations.cropManagement.date,
            season_planning_id = entityWithRelations.cropManagement.seasonPlanningId,
            comments = entityWithRelations.cropManagement.comments,
            gappingActivity = entityWithRelations.gappingActivities.firstOrNull()
                ?.let { convertFromGappingEntity(it) }
                ?: createDefaultGappingActivity(),
            weedingActivity = entityWithRelations.weedingActivities.firstOrNull()
                ?.let { convertFromWeedingEntity(it) }
                ?: createDefaultWeedingActivity(),
            pruningActivity = entityWithRelations.pruningActivities.firstOrNull()
                ?.let { convertFromPruningEntity(it) }
                ?: createDefaultPruningActivity(),
            stakingActivity = entityWithRelations.stakingActivities.firstOrNull()
                ?.let { convertFromStakingEntity(it) }
                ?: createDefaultStakingActivity(),
            thinningActivity = entityWithRelations.thinningActivities.firstOrNull()
                ?.let { convertFromThinningEntity(it) }
                ?: createDefaultThinningActivity(),
            wateringActivity = entityWithRelations.wateringActivities.firstOrNull()
                ?.let { convertFromWateringEntity(it) }
                ?: createDefaultWateringActivity()
        )
    }

    private fun createDefaultGappingActivity() = GappingActivity(
        activity = "Gapping",
        crop_population = 0,
        man_days = 0,
        planting_material = "",
        target_population = 0,
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultWeedingActivity() = WeedingActivity(
        activity = "Weeding",
        input = "",
        man_days = 0,
        method_of_weeding = "",
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultPruningActivity() = PruningActivity(
        activity = "Pruning",
        equipment_used = "",
        man_days = 0,
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultStakingActivity() = StakingActivity(
        activity = "Staking",
        cost_per_unit = 0.0,
        man_days = 0,
        unit_cost_of_labor = 0.0,
        unit_stakes = 0
    )

    private fun createDefaultThinningActivity() = ThinningActivity(
        activity = "Thinning",
        equipment_used = "",
        man_days = 0,
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultWateringActivity() = WateringActivity(
        activity = "Watering",
        cost_of_fuel = 0.0,
        discharge_hours = 0,
        end_time = "",
        frequency_of_watering = "",
        man_days = 0,
        start_time = "",
        type_of_irrigation = "",
        unit_cost = 0.0,
        unit_cost_of_labor = 0.0
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

}
