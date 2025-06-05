package com.example.farmdatapod.logistics.equipments.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.logistics.equipments.data.EquipmentEntity
import com.example.farmdatapod.logistics.equipments.models.EquipmentModel
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

class EquipmentRepository(private val context: Context) : SyncableRepository {
    private val TAG = "EquipmentRepository"
    private val equipmentDao = AppDatabase.getInstance(context).equipmentDao()

    suspend fun saveEquipment(
        equipment: EquipmentEntity,
        isOnline: Boolean
    ): Result<EquipmentEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(
                    TAG,
                    "Attempting to save equipment: ${equipment.dn_number}, Online mode: $isOnline"
                )

                // Check if equipment already exists
                val existingEquipment = equipmentDao.getEquipmentByFields(
                    equipment.journey_id,
                    equipment.stop_point_id,
                    equipment.equipment,
                    equipment.dn_number
                )

                val localId: Long
                if (existingEquipment == null) {
                    localId = equipmentDao.insert(equipment)
                    Log.d(TAG, "Equipment saved locally with ID: $localId")
                } else {
                    localId = existingEquipment.id.toLong()
                    Log.d(TAG, "Equipment already exists locally with ID: ${existingEquipment.id}")
                    updateEquipmentAndMarkForSync(existingEquipment)
                }

                if (isOnline) {
                    try {
                        Log.d(TAG, "Attempting to sync with server...")
                        val equipmentModel = convertToModel(equipment)

                        context?.let { ctx ->
                            RestClient.getApiService(ctx).planJourneyEquipment(equipmentModel)
                                .enqueue(object : Callback<EquipmentModel> {
                                    override fun onResponse(
                                        call: Call<EquipmentModel>,
                                        response: Response<EquipmentModel>
                                    ) {
                                        if (response.isSuccessful) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                equipmentDao.markAsSynced(localId.toInt())
                                                Log.d(TAG, "Equipment synced with server")
                                            }
                                        } else {
                                            Log.e(
                                                TAG,
                                                "Sync failed: ${response.errorBody()?.string()}"
                                            )
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<EquipmentModel>,
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
                    Log.d(TAG, "Offline mode - Equipment will sync later")
                }
                Result.success(equipment)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving equipment", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedEquipment = equipmentDao.getUnsyncedEquipment()
            Log.d(TAG, "Found ${unsyncedEquipment.size} unsynced equipment")

            var successCount = 0
            var failureCount = 0

            unsyncedEquipment.forEach { equipment ->
                try {
                    val equipmentModel = convertToModel(equipment)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).planJourneyEquipment(equipmentModel)
                            .enqueue(object : Callback<EquipmentModel> {
                                override fun onResponse(
                                    call: Call<EquipmentModel>,
                                    response: Response<EquipmentModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            equipmentDao.markAsSynced(equipment.id)
                                            successCount++
                                            Log.d(
                                                TAG,
                                                "Successfully synced equipment ${equipment.dn_number}"
                                            )
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(
                                            TAG,
                                            "Failed to sync equipment ${equipment.dn_number}"
                                        )
                                    }
                                }

                                override fun onFailure(call: Call<EquipmentModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing equipment ${equipment.dn_number}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing equipment ${equipment.dn_number}", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            context?.let { ctx ->
                RestClient.getApiService(ctx).getJourneyEquipment()
                    .enqueue(object : Callback<List<EquipmentModel>> {
                        override fun onResponse(
                            call: Call<List<EquipmentModel>>,
                            response: Response<List<EquipmentModel>>
                        ) {
                            if (response.isSuccessful) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    var savedCount = 0
                                    response.body()?.forEach { equipmentModel ->
                                        try {
                                            val entity = convertToEntity(equipmentModel)
                                            equipmentDao.insert(entity)
                                            savedCount++
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error saving equipment from server", e)
                                        }
                                    }
                                    continuation.resume(Result.success(savedCount))
                                }
                            } else {
                                continuation.resume(Result.failure(RuntimeException("Server sync failed")))
                            }
                        }

                        override fun onFailure(call: Call<List<EquipmentModel>>, t: Throwable) {
                            continuation.resume(Result.failure(t))
                        }
                    })
            } ?: continuation.resume(Result.failure(RuntimeException("Context is null")))
        }
    }

    private suspend fun updateEquipmentAndMarkForSync(equipment: EquipmentEntity) {
        equipmentDao.update(equipment)
        equipmentDao.markForSync(equipment.id)
    }

    private fun convertToModel(entity: EquipmentEntity): EquipmentModel {
        return EquipmentModel(
            description = entity.description,
            dn_number = entity.dn_number,
            equipment = entity.equipment,
            journey_id = entity.journey_id,
            number_of_units = entity.number_of_units,
            stop_point_id = entity.stop_point_id,
            unit_cost = entity.unit_cost
        )
    }

    private fun convertToEntity(model: EquipmentModel): EquipmentEntity {
        return EquipmentEntity(
            description = model.description,
            dn_number = model.dn_number,
            equipment = model.equipment,
            journey_id = model.journey_id,
            number_of_units = model.number_of_units,
            stop_point_id = model.stop_point_id,
            unit_cost = model.unit_cost,
            syncStatus = true,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
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

    fun getAllEquipment(): Flow<List<EquipmentEntity>> = equipmentDao.getAllEquipment()

    suspend fun getEquipmentById(id: Int): EquipmentEntity? = withContext(Dispatchers.IO) {
        equipmentDao.getEquipmentById(id)
    }

    suspend fun deleteEquipment(equipment: EquipmentEntity) = withContext(Dispatchers.IO) {
        equipmentDao.delete(equipment)
    }

    suspend fun updateEquipment(equipment: EquipmentEntity) = withContext(Dispatchers.IO) {
        equipmentDao.update(equipment)
    }
}

