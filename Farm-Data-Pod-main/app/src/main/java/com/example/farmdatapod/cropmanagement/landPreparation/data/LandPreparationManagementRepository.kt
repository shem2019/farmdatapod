package com.example.farmdatapod.cropmanagement.landPreparation.data


import com.example.farmdatapod.models.CoverCrop
import com.example.farmdatapod.models.LandPreparationMulching
import com.example.farmdatapod.models.LandPreparationSoilAnalysis
import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.LandPreparationModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.season.landPreparation.data.CoverCropEntity
import com.example.farmdatapod.season.landPreparation.data.LandPreparationEntity
import com.example.farmdatapod.season.landPreparation.data.LandPreparationWithDetails
import com.example.farmdatapod.season.landPreparation.data.MulchingEntity
import com.example.farmdatapod.season.landPreparation.data.SoilAnalysisEntity
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

class LandPreparationManagementRepository(private val context: Context) : SyncableRepository {
    private val TAG = "LandPrepManagementRepo"
    private val landPrepDao = AppDatabase.getInstance(context).landPreparationDao()

    suspend fun saveLandPreparation(
        landPrep: LandPreparationEntity,
        coverCrop: CoverCropEntity?,
        mulching: MulchingEntity?,
        soilAnalysis: SoilAnalysisEntity?,
        isOnline: Boolean
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save land preparation for Producer ${landPrep.producerId}, Online mode: $isOnline")

            // Save to local database first
            val localId = landPrepDao.insertLandPreparationWithDetails(
                landPrep,
                coverCrop,
                mulching,
                soilAnalysis
            )

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val requestModel = createLandPrepModel(landPrep, coverCrop, mulching, soilAnalysis)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).landPreparationManagement(requestModel)
                            .enqueue(object : Callback<LandPreparationModel> {
                                override fun onResponse(
                                    call: Call<LandPreparationModel>,
                                    response: Response<LandPreparationModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverResponse ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                landPrepDao.updateSyncStatus(localId, true)
                                                Log.d(TAG, "Land preparation synced successfully")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<LandPreparationModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            }

            Result.success(localId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving land preparation", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedPreps = landPrepDao.getUnsyncedLandPreparations().first()
            Log.d(TAG, "Found ${unsyncedPreps.size} unsynced land preparations")

            var successCount = 0
            var failureCount = 0

            unsyncedPreps.forEach { landPrepWithDetails ->
                try {
                    val requestModel = createLandPrepModel(
                        landPrepWithDetails.landPrep,
                        landPrepWithDetails.coverCrop,
                        landPrepWithDetails.mulching,
                        landPrepWithDetails.soilAnalysis
                    )

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).landPreparationManagement(requestModel)
                            .enqueue(object : Callback<LandPreparationModel> {
                                override fun onResponse(
                                    call: Call<LandPreparationModel>,
                                    response: Response<LandPreparationModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            landPrepDao.updateSyncStatus(landPrepWithDetails.landPrep.id, true)
                                            successCount++
                                            Log.d(TAG, "Successfully synced land preparation ID: ${landPrepWithDetails.landPrep.id}")
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync land preparation ID: ${landPrepWithDetails.landPrep.id}")
                                    }
                                }

                                override fun onFailure(call: Call<LandPreparationModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing land preparation", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing land preparation", e)
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
                    RestClient.getApiService(ctx).getLandPreparationManagement()
                        .enqueue(object : Callback<List<LandPreparationModel>> {
                            override fun onResponse(
                                call: Call<List<LandPreparationModel>>,
                                response: Response<List<LandPreparationModel>>
                            ) {
                                if (response.isSuccessful) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            var savedCount = 0
                                            response.body()?.forEach { serverModel ->
                                                val entities = convertModelToEntities(serverModel)
                                                landPrepDao.insertLandPreparationWithDetails(
                                                    entities.first,
                                                    entities.second,
                                                    entities.third,
                                                    entities.fourth
                                                )
                                                savedCount++
                                            }
                                            continuation.resume(Result.success(savedCount))
                                        } catch (e: Exception) {
                                            continuation.resume(Result.failure(e))
                                        }
                                    }
                                } else {
                                    continuation.resume(Result.failure(RuntimeException("Server error: ${response.code()}")))
                                }
                            }

                            override fun onFailure(call: Call<List<LandPreparationModel>>, t: Throwable) {
                                continuation.resume(Result.failure(t))
                            }
                        })
                } ?: continuation.resume(Result.failure(RuntimeException("Context is null")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createLandPrepModel(
        landPrep: LandPreparationEntity,
        coverCrop: CoverCropEntity?,
        mulching: MulchingEntity?,
        soilAnalysis: SoilAnalysisEntity?
    ): LandPreparationModel {
        return LandPreparationModel(
            producer = landPrep.producerId.toString(),
            season = landPrep.seasonId.toString(),
            field = landPrep.fieldNumber,
            dateOfLandPreparation = landPrep.dateOfLandPreparation,
            methodOfLandPreparation = landPrep.methodOfLandPreparation,
            coverCrop = coverCrop?.let {
                CoverCrop(
                    coverCrop = it.coverCrop,
                    dateOfEstablishment = it.dateOfEstablishment,
                    unit = it.unit,
                    unitCost = it.unitCost,
                    typeOfInoculant = it.typeOfInoculant,
                    dateOfIncorporation = it.dateOfIncorporation,
                    manDays = it.manDays,
                    unitCostOfLabor = it.unitCostOfLabor
                )
            },
            mulching = mulching?.let {
                LandPreparationMulching(
                    typeOfMulch = it.typeOfMulch,
                    costOfMulch = it.costOfMulch,
                    lifeCycleOfMulchInSeasons = it.lifeCycleOfMulchInSeasons,
                    manDays = it.manDays,
                    unitCostOfLabor = it.unitCostOfLabor
                )
            },
            soilAnalysis = soilAnalysis?.let {
                LandPreparationSoilAnalysis(
                    typeOfAnalysis = it.typeOfAnalysis,
                    costOfAnalysis = it.costOfAnalysis,
                    lab = it.lab
                )
            },
            season_planning_id = landPrep.season_planning_id
        )
    }

    private fun convertModelToEntities(model: LandPreparationModel): Quadruple<
            LandPreparationEntity,
            CoverCropEntity?,
            MulchingEntity?,
            SoilAnalysisEntity?
            > {
        val landPrep = LandPreparationEntity(
            producerId = model.producer.toInt(),
            seasonId = model.season.toLong(),
            fieldNumber = model.field,
            dateOfLandPreparation = model.dateOfLandPreparation,
            methodOfLandPreparation = model.methodOfLandPreparation,
            season_planning_id = model.season_planning_id,
            syncStatus = true
        )

        val coverCrop = model.coverCrop?.let {
            CoverCropEntity(
                landPrepId = 0,
                coverCrop = it.coverCrop,
                dateOfEstablishment = it.dateOfEstablishment,
                unit = it.unit,
                unitCost = it.unitCost,
                typeOfInoculant = it.typeOfInoculant,
                dateOfIncorporation = it.dateOfIncorporation,
                manDays = it.manDays,
                unitCostOfLabor = it.unitCostOfLabor
            )
        }

        val mulching = model.mulching?.let {
            MulchingEntity(
                landPrepId = 0,
                typeOfMulch = it.typeOfMulch,
                costOfMulch = it.costOfMulch,
                lifeCycleOfMulchInSeasons = it.lifeCycleOfMulchInSeasons,
                manDays = it.manDays,
                unitCostOfLabor = it.unitCostOfLabor
            )
        }

        val soilAnalysis = model.soilAnalysis?.let {
            SoilAnalysisEntity(
                landPrepId = 0,
                typeOfAnalysis = it.typeOfAnalysis,
                costOfAnalysis = it.costOfAnalysis,
                lab = it.lab
            )
        }

        return Quadruple(landPrep, coverCrop, mulching, soilAnalysis)
    }

    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
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

    // Database operations
    fun getAllLandPreparations(): Flow<List<LandPreparationWithDetails>> =
        landPrepDao.getAllLandPreparations()

    fun getLandPreparationsByProducer(producerId: Int): Flow<List<LandPreparationWithDetails>> =
        landPrepDao.getLandPreparationsByProducer(producerId)

    suspend fun getLandPreparationById(id: Long): LandPreparationWithDetails? =
        landPrepDao.getLandPreparationById(id)

    suspend fun deleteLandPreparation(landPrep: LandPreparationEntity) =
        landPrepDao.deleteLandPreparation(landPrep)
}