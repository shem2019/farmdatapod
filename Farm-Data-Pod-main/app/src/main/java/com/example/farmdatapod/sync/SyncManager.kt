package com.example.farmdatapod.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.farmdatapod.cropmanagement.cropManagementActivities.data.CropManagementActivityRepository
import com.example.farmdatapod.cropmanagement.cropProtection.data.CropProtectionManagementRepository
import com.example.farmdatapod.cropmanagement.forecastYield.data.YieldForecastManagementRepository
import com.example.farmdatapod.cropmanagement.germination.data.GerminationManagementRepository
import com.example.farmdatapod.cropmanagement.harvesting.data.HarvestManagementRepository
import com.example.farmdatapod.cropmanagement.landPreparation.data.LandPreparationManagementRepository
import com.example.farmdatapod.cropmanagement.nursery.data.NurseryManagementRepository
import com.example.farmdatapod.cropmanagement.nutrition.data.CropNutritionManagementRepository
import com.example.farmdatapod.cropmanagement.scouting.data.BaitManagementRepository
import com.example.farmdatapod.hub.hubAggregation.buyingCenter.data.BuyingCenterRepository
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIGRepository
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.logistics.createRoute.data.RouteRepository
import com.example.farmdatapod.logistics.equipments.data.EquipmentRepository
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingRepository
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data.LoadingInputRepository
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsRepository
import com.example.farmdatapod.logistics.inputTransfer.data.InputTransferRepository
import com.example.farmdatapod.logistics.planJourney.data.JourneyRepository
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.cropManagement.data.CropManagementRepository
import com.example.farmdatapod.season.cropProtection.data.CropProtectionRepository
import com.example.farmdatapod.season.forecastYields.data.YieldForecastRepository
import com.example.farmdatapod.season.germination.data.GerminationRepository
import com.example.farmdatapod.season.harvest.data.HarvestPlanningRepository
import com.example.farmdatapod.season.landPreparation.data.LandPreparationRepository
import com.example.farmdatapod.season.nursery.data.NurseryPlanningRepository
import com.example.farmdatapod.season.nutrition.data.CropNutritionRepository
import com.example.farmdatapod.season.planting.data.PlanPlantingRepository
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.season.scouting.data.BaitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {
    private val TAG = "SyncManager"
    private val workManager = WorkManager.getInstance(context)


    // List of all repositories that need syncing
    private val repositories = listOf<Pair<String, SyncableRepository>>(
        "Seasons" to SeasonRepository(context),
        "YieldForecasts" to YieldForecastRepository(context),
        "Germinations" to GerminationRepository(context),
        "Baits" to BaitRepository(context),
        "planting" to PlanPlantingRepository(context),
        "hub" to HubRepository(context),
        "buying_centers" to BuyingCenterRepository(context),
        "CIGs" to CIGRepository(context),
        "Producers" to ProducerRepository(context),
        "FieldRegistrations" to FieldRegistrationRepository(context),
        "NurseryPlans" to NurseryPlanningRepository(context),
        "LandPreparations" to LandPreparationRepository(context),
        "Crop Management" to CropManagementRepository(context),
        "Crop Nutrition" to CropNutritionRepository(context),
        "Crop Protection" to CropProtectionRepository(context),
        "Crop Harvest" to HarvestPlanningRepository(context),
        "Nursery Management" to NurseryManagementRepository(context),
        "Land Preparation Management" to LandPreparationManagementRepository(context),
        "Germination Management" to GerminationManagementRepository(context),
        "Yield Forecast Management" to YieldForecastManagementRepository(context),
        "Crop Management" to CropManagementActivityRepository(context),
        "Crop Nutrition Management" to CropNutritionManagementRepository(context),
        "Crop Protection Management" to CropProtectionManagementRepository(context),
        "Harvest Management" to HarvestManagementRepository(context),
        "Scouting Management" to BaitManagementRepository(context),
        "Route" to RouteRepository(context),
        "plan Journey" to JourneyRepository(context),
        "Equipment" to EquipmentRepository(context),
        "Input Allocation" to PlanJourneyInputsRepository(context),
        "Input Transfer" to InputTransferRepository(context),
        "Equipment Loading" to EquipmentLoadingRepository(context),
        "Loading Input" to LoadingInputRepository(context)

        // Add other repositories here as needed:
        // "Nursery" to NurseryRepository(context),
        // "LandPrep" to LandPreparationRepository(context),
        // etc.
    )

    companion object {
        private const val AUTO_SYNC_WORK = "auto_sync_work"
    }

    fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val autoSyncRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AUTO_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            autoSyncRequest
        )

        Log.d(TAG, "Periodic sync scheduled")
    }

    fun cancelAllSync() {
        workManager.cancelUniqueWork(AUTO_SYNC_WORK)
        Log.d(TAG, "All sync cancelled")
    }

    // Replace old syncNow with suspend function that performs sync directly
    suspend fun syncNow(): SyncResult {
        Log.d(TAG, "Starting immediate sync")
        return performFullSync()
    }

    private suspend fun syncRepository(
        entityName: String,
        repository: SyncableRepository,
        results: MutableList<EntitySyncResult>
    ) {
        try {
            Log.d(TAG, "Starting sync for $entityName")
            repository.performFullSync().fold(
                onSuccess = { stats ->
                    results.add(
                        EntitySyncResult(
                            entityName = entityName,
                            uploadedCount = stats.uploadedCount,
                            downloadedCount = stats.downloadedCount,
                            failures = stats.uploadFailures,
                            successful = stats.successful
                        )
                    )
                    Log.d(TAG, "Successfully synced $entityName")
                },
                onFailure = { error ->
                    results.add(
                        EntitySyncResult(
                            entityName = entityName,
                            error = error.message
                        )
                    )
                    Log.e(TAG, "Failed to sync $entityName: ${error.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing $entityName", e)
            results.add(
                EntitySyncResult(
                    entityName = entityName,
                    error = e.message
                )
            )
        }
    }

    suspend fun performFullSync(): SyncResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<EntitySyncResult>()
        var overallError: String? = null

        try {
            Log.d(TAG, "Starting full sync for ${repositories.size} repositories")

            repositories.forEach { (entityName, repository) ->
                syncRepository(entityName, repository, results)
            }

            Log.d(TAG, "Full sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during full sync", e)
            overallError = e.message
        }

        SyncResult(results, overallError)
    }
}