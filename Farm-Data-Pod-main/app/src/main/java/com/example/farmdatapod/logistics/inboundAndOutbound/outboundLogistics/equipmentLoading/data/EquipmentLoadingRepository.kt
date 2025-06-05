package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data
import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.model.LoadedEquipmentModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.await
import java.util.concurrent.TimeUnit

class EquipmentLoadingRepository(context: Context) : SyncableRepository {
    private val TAG = "EquipmentLoadingRepository"
    private val database = AppDatabase.getInstance(context)
    private val equipmentLoadingDao = database.equipmentLoadingDao()
    private val apiService = RestClient.getApiService(context)

    suspend fun saveEquipmentLoading(equipmentLoading: EquipmentLoadingEntity, isOnline: Boolean): Result<EquipmentLoadingEntity> =
        withContext(Dispatchers.IO) {
            try {
                database.withTransaction {
                    Log.d(TAG, "Starting transaction for equipment loading: ${equipmentLoading.delivery_note_number}")

                    val existingLoading = equipmentLoadingDao.getEquipmentLoadingByDeliveryNote(equipmentLoading.delivery_note_number)
                    val localId = if (existingLoading == null) {
                        equipmentLoadingDao.insert(equipmentLoading)
                    } else {
                        updateEquipmentLoadingAndMarkForSync(existingLoading)
                        existingLoading.id.toLong()
                    }

                    if (isOnline) {
                        try {
                            withTimeout(TimeUnit.SECONDS.toMillis(30)) {
                                val loadedEquipmentModel = createLoadedEquipmentModel(equipmentLoading)
                                val response = apiService.loadedEquipment(loadedEquipmentModel).await()

                                equipmentLoadingDao.updateSyncStatus(
                                    localId.toInt(),
                                    true,
                                    System.currentTimeMillis(),
                                    localId
                                )
                                Log.d(TAG, "Equipment loading synced successfully")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during server sync", e)
                            // Mark for future sync instead of failing
                            equipmentLoadingDao.markForSync(localId.toInt())
                        }
                    }

                    Result.success(equipmentLoading)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving equipment loading", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            var failureCount = 0

            database.withTransaction {
                val unsyncedLoadings = equipmentLoadingDao.getUnsyncedEquipmentLoadings().first()

                unsyncedLoadings.forEach { loading ->
                    try {
                        withTimeout(TimeUnit.SECONDS.toMillis(30)) {
                            val loadedEquipmentModel = createLoadedEquipmentModel(loading)
                            val response = apiService.loadedEquipment(loadedEquipmentModel).await()

                            equipmentLoadingDao.updateSyncStatus(
                                loading.id,
                                true,
                                System.currentTimeMillis(),
                                loading.server_id
                            )
                            successCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing ${loading.delivery_note_number}", e)
                        failureCount++
                    }
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
            database.withTransaction {
                val serverLoadings = apiService.getLoadedEquipment().await()
                var savedCount = 0

                serverLoadings.forEach { serverLoading ->
                    try {
                        val entity = createEntityFromServer(serverLoading)
                        equipmentLoadingDao.insert(entity)
                        savedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving loading from server", e)
                    }
                }

                Result.success(savedCount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    private suspend fun updateEquipmentLoadingAndMarkForSync(equipmentLoading: EquipmentLoadingEntity) {
        database.withTransaction {
            equipmentLoadingDao.update(equipmentLoading)
            equipmentLoadingDao.markForSync(equipmentLoading.id)
        }
    }

    private fun createLoadedEquipmentModel(entity: EquipmentLoadingEntity) = LoadedEquipmentModel(
        authorised = entity.authorised,
        delivery_note_number = entity.delivery_note_number,
        equipment = entity.equipment,
        journey_id = entity.journey_id,
        quantity_loaded = entity.quantity_loaded,
        stop_point_id = entity.stop_point_id
    )

    private fun createEntityFromServer(model: LoadedEquipmentModel) = EquipmentLoadingEntity(
        server_id = 0L, // This would need to be set from the server response if available
        authorised = model.authorised,
        delivery_note_number = model.delivery_note_number,
        dn_number = model.delivery_note_number,  // Added this, using same as delivery_note_number
        equipment = model.equipment,
        journey_id = model.journey_id,
        quantity_loaded = model.quantity_loaded,
        number_of_units = model.quantity_loaded,  // Added this, assuming initial available units equals loaded
        stop_point_id = model.stop_point_id,
        syncStatus = true,
        lastModified = System.currentTimeMillis(),
        lastSynced = System.currentTimeMillis()
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

    fun getAllEquipmentLoadings(): Flow<List<EquipmentLoadingEntity>> =
        equipmentLoadingDao.getAllEquipmentLoadings()

    suspend fun getEquipmentLoadingById(id: Int): EquipmentLoadingEntity? = withContext(Dispatchers.IO) {
        equipmentLoadingDao.getEquipmentLoadingById(id)
    }

    fun getEquipmentLoadingsByJourneyId(journeyId: Int): Flow<List<EquipmentLoadingEntity>> =
        equipmentLoadingDao.getEquipmentLoadingsByJourneyId(journeyId)

    suspend fun deleteEquipmentLoading(equipmentLoading: EquipmentLoadingEntity) = withContext(Dispatchers.IO) {
        equipmentLoadingDao.delete(equipmentLoading)
    }
}