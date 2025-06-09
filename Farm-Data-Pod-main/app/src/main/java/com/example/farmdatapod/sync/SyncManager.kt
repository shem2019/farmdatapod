package com.example.farmdatapod.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
    // CORRECTED: Removed the duplicate "hub" entry
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
        "Crop Management Activity" to CropManagementActivityRepository(context),
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
    )

    companion object {
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
        private const val IMMEDIATE_SYNC_WORK_NAME = "immediate_sync_work"
    }

    /**
     * Schedules a background sync to run periodically (e.g., every 15 mins)
     * when the network is available. This is a reliable fallback.
     */
    fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Use KEEP to prevent the timer from resetting on every app start
            periodicSyncRequest
        )
        Log.i(TAG, "Periodic background sync worker has been scheduled.")
    }

    /**
     * The new utility to trigger an immediate sync.
     * This schedules a one-time job that runs as soon as network is available.
     * Call this function right after saving new data locally (e.g., from your ViewModel).
     */
    fun triggerImmediateSync() {
        Log.i(TAG, "An immediate sync has been requested.")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateSyncRequest = OneTimeWorkRequestBuilder<AutoSyncWorker>()
            .setConstraints(constraints)
            .build()

        // Enqueue the work as unique. If another immediate sync is already pending,
        // this new one will replace it, preventing a pile-up of sync requests.
        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateSyncRequest
        )
        Log.d(TAG, "Immediate sync job enqueued. WorkManager will run it once network constraints are met.")
    }

    /**
     * Cancels all scheduled sync jobs. Call this on user logout.
     */
    fun cancelAllSync() {
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_SYNC_WORK_NAME)
        Log.w(TAG, "All periodic and immediate sync jobs have been cancelled.")
    }

    /**
     * Performs a direct, blocking sync. Called by the AutoSyncWorker.
     */
    suspend fun performFullSync(): SyncResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<EntitySyncResult>()
        var overallError: String? = null

        try {
            Log.i(TAG, "========= Starting Full Sync Process (Serial Execution) =========")
            repositories.forEach { (entityName, repository) ->
                Log.d(TAG, "--- Now syncing: $entityName ---")
                syncRepository(entityName, repository, results)
            }
            Log.i(TAG, "========= SERIAL full sync process completed. ==========")
        } catch (e: Exception) {
            Log.e(TAG, "A critical error occurred during the full sync process.", e)
            overallError = e.message
        }
        SyncResult(results, overallError)
    }

    /**
     * Helper function to sync a single repository and record the outcome.
     */
    private suspend fun syncRepository(
        entityName: String,
        repository: SyncableRepository,
        results: MutableList<EntitySyncResult>
    ) {
        try {
            Log.d(TAG, "Calling performFullSync() for $entityName repository.")
            repository.performFullSync().fold(
                onSuccess = { stats ->
                    synchronized(results) {
                        results.add(
                            EntitySyncResult(
                                entityName = entityName,
                                uploadedCount = stats.uploadedCount,
                                downloadedCount = stats.downloadedCount,
                                failures = stats.uploadFailures,
                                successful = stats.successful
                            )
                        )
                    }
                    if (!stats.successful) {
                        Log.w(TAG, "Sync for $entityName completed with failures. Uploaded: ${stats.uploadedCount}, Failed: ${stats.uploadFailures}")
                    } else {
                        Log.d(TAG, "Successfully synced $entityName. Uploaded: ${stats.uploadedCount}, Downloaded: ${stats.downloadedCount}")
                    }
                },
                onFailure = { error ->
                    synchronized(results) {
                        results.add(EntitySyncResult(entityName = entityName, error = error.message))
                    }
                    Log.e(TAG, "Failed to sync $entityName: ${error.message}", error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "An exception occurred while syncing $entityName", e)
            synchronized(results) {
                results.add(EntitySyncResult(entityName = entityName, error = e.message))
            }
        }
    }

    // The suspend fun syncNow() is fine as it is, it's just a wrapper for performFullSync
    suspend fun syncNow(): SyncResult {
        Log.d(TAG, "Starting immediate on-demand sync via syncNow()")
        return performFullSync()
    }
}