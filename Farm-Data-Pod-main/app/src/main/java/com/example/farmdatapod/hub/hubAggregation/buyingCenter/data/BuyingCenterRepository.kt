package com.example.farmdatapod.hub.hubAggregation.buyingCenter.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.BuyingCenter
import com.example.farmdatapod.dbmodels.BuyingCenterResponse
import com.example.farmdatapod.models.BuyingCentreRequest
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BuyingCenterRepository(private val context: Context) : SyncableRepository {
    private val TAG = "BuyingCenterRepository"
    private val buyingCenterDao = AppDatabase.getInstance(context).buyingCenterDao()
    private val hubDao = AppDatabase.getInstance(context).hubDao()
    private val apiService = RestClient.getApiService(context)

    private suspend fun getHubIdByName(hubName: String): Int? {
        return hubDao.getHubByName(hubName)?.id
    }

    suspend fun saveBuyingCenter(buyingCenter: BuyingCenterEntity, isOnline: Boolean): Result<BuyingCenterEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to save buying center for hub ${buyingCenter.hub}: ${buyingCenter.buyingCenterName}, Online mode: $isOnline")

                // Verify hub exists before saving
                val hub = buyingCenter.hub?.let { hubDao.getHubByName(it) }
                    ?: return@withContext Result.failure(Exception("Selected hub does not exist"))

                // Set the hubId in the buying center entity
                val buyingCenterWithHubId = buyingCenter.copy(
                    hubId = hub.id,
                    syncStatus = false,
                    lastModified = System.currentTimeMillis()
                )

                // First save locally
                val localId = buyingCenterDao.insert(buyingCenterWithHubId)
                Log.d(TAG, "Buying center saved locally with ID: $localId")

                if (isOnline) {
                    try {
                        Log.d(TAG, "Attempting to sync with server...")
                        val buyingCenterRequest = createBuyingCenterRequest(buyingCenterWithHubId)
                        val response = buyingCenterRequest?.let { apiService.registerBuyingCentre(it).execute() }

                        if (response != null) {
                            if (response.isSuccessful) {
                                Log.d(TAG, "Server response body: ${response.body()}")
                                buyingCenterDao.updateSyncStatus(localId.toInt(), true)
                                Log.d(TAG, "Buying center synced successfully")
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                                buyingCenterDao.updateSyncStatus(localId.toInt(), false)
                                Log.e(TAG, "Server sync failed: $errorMsg")
                            }
                        }
                    } catch (e: Exception) {
                        buyingCenterDao.updateSyncStatus(localId.toInt(), false)
                        Log.e(TAG, "Error during server sync", e)
                    }
                } else {
                    Log.d(TAG, "Offline mode - Buying center will sync later")
                }

                Result.success(buyingCenterWithHubId)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving buying center", e)
                Result.failure(e)
            }
        }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedCenters = buyingCenterDao.getBuyingCentersByStatus(false)
            Log.d(TAG, "Found ${unsyncedCenters.size} unsynced buying centers")

            var successCount = 0
            var failureCount = 0

            unsyncedCenters.forEach { center ->
                try {
                    val centerRequest = createBuyingCenterRequest(center)
                    val response = centerRequest?.let { apiService.registerBuyingCentre(it).execute() }

                    if (response != null) {
                        if (response.isSuccessful) {
                            buyingCenterDao.updateSyncStatus(center.id, true)
                            successCount++
                            Log.d(TAG, "Successfully synced ${center.buyingCenterName}")
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                            buyingCenterDao.updateSyncStatus(center.id, false)
                            failureCount++
                            Log.e(TAG, "Failed to sync ${center.buyingCenterName}: $errorMsg")
                        }
                    }
                } catch (e: Exception) {
                    buyingCenterDao.updateSyncStatus(center.id, false)
                    failureCount++
                    Log.e(TAG, "Error syncing ${center.buyingCenterName}", e)
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
            Log.d(TAG, "Starting server sync")

            return@withContext suspendCoroutine { continuation ->
                apiService.getBuyingCenters(null)?.enqueue(object : Callback<BuyingCenterResponse> {
                    override fun onResponse(
                        call: Call<BuyingCenterResponse>,
                        response: Response<BuyingCenterResponse>
                    ) {
                        if (!response.isSuccessful) {
                            continuation.resume(Result.failure(Exception("Server error: ${response.code()}")))
                            return
                        }

                        val serverResponse = response.body()
                        if (serverResponse == null) {
                            continuation.resume(Result.success(0))
                            return
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                // Get existing records to preserve
                                val unsynced = buyingCenterDao.getUnsynced()
                                val existingIds = buyingCenterDao.getAllServerIds()

                                var savedCount = 0
                                serverResponse.forms.forEach { serverCenter ->
                                    try {
                                        val hubId = serverCenter.hub?.let { getHubIdByName(it) }
                                            ?: return@forEach

                                        val entity = createEntityFromResponse(serverCenter, hubId)

                                        if (existingIds.contains(serverCenter.id)) {
                                            buyingCenterDao.update(entity)
                                        } else {
                                            buyingCenterDao.insert(entity)
                                        }
                                        savedCount++
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error processing center", e)
                                    }
                                }

                                // Restore unsynced records
                                unsynced.forEach { unsyncedCenter ->
                                    if (unsyncedCenter.serverId == null) {
                                        buyingCenterDao.insert(unsyncedCenter)
                                    }
                                }

                                Log.d(TAG, "Sync completed: $savedCount centers processed")
                                continuation.resume(Result.success(savedCount))
                            } catch (e: Exception) {
                                Log.e(TAG, "Processing error", e)
                                continuation.resume(Result.failure(e))
                            }
                        }
                    }

                    override fun onFailure(call: Call<BuyingCenterResponse>, t: Throwable) {
                        Log.e(TAG, "Network error", t)
                        continuation.resume(Result.failure(t))
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            Result.failure(e)
        }
    }

    private fun createEntityFromResponse(
        buyingCenter: BuyingCenter,  // Changed to your actual BuyingCenter class
        hubId: Int
    ): BuyingCenterEntity = BuyingCenterEntity(
        serverId = buyingCenter.id,
        hubId = hubId,
        hub = buyingCenter.hub,
        county = buyingCenter.county ?: "",
        subCounty = buyingCenter.sub_county ?: "",
        ward = buyingCenter.ward ?: "",
        village = buyingCenter.village ?: "",
        buyingCenterName = buyingCenter.buying_center_name ?: "",
        buyingCenterCode = buyingCenter.buying_center_code ?: "",
        address = buyingCenter.address ?: "",
        yearEstablished = buyingCenter.year_established ?: "",
        ownership = buyingCenter.ownership ?: "",
        floorSize = buyingCenter.floor_size ?: "",
        facilities = buyingCenter.facilities ?: "",
        inputCenter = buyingCenter.input_center ?: "",
        typeOfBuilding = buyingCenter.type_of_building ?: "",
        location = buyingCenter.location ?: "",
        userId = buyingCenter.user_id,
        syncStatus = true,
        contactOtherName = buyingCenter.key_contacts?.firstOrNull()?.other_name ?: "",
        contactLastName = buyingCenter.key_contacts?.firstOrNull()?.last_name ?: "",
        contactGender = buyingCenter.key_contacts?.firstOrNull()?.gender ?: "",
        contactRole = buyingCenter.key_contacts?.firstOrNull()?.role ?: "",
        contactDateOfBirth = buyingCenter.key_contacts?.firstOrNull()?.date_of_birth ?: "",
        contactEmail = buyingCenter.key_contacts?.firstOrNull()?.email ?: "",
        contactPhoneNumber = buyingCenter.key_contacts?.firstOrNull()?.phone_number ?: "",
        contactIdNumber = buyingCenter.key_contacts?.firstOrNull()?.id_number ?: 0,
        lastModified = System.currentTimeMillis()
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

    private fun createBuyingCenterRequest(buyingCenter: BuyingCenterEntity) = buyingCenter.hub?.let {
        BuyingCentreRequest(
            hub = it,
            county = buyingCenter.county,
            sub_county = buyingCenter.subCounty,
            ward = buyingCenter.ward,
            village = buyingCenter.village,
            buying_center_name = buyingCenter.buyingCenterName,
            buying_center_code = buyingCenter.buyingCenterCode,
            address = buyingCenter.address,
            year_established = buyingCenter.yearEstablished,
            ownership = buyingCenter.ownership,
            floor_size = buyingCenter.floorSize,
            facilities = buyingCenter.facilities,
            input_center = buyingCenter.inputCenter,
            type_of_building = buyingCenter.typeOfBuilding,
            location = buyingCenter.location,
            key_contacts = listOf(
                com.example.farmdatapod.models.KeyContact(
                    other_name = buyingCenter.contactOtherName,
                    last_name = buyingCenter.contactLastName,
                    gender = buyingCenter.contactGender,
                    role = buyingCenter.contactRole,
                    date_of_birth = buyingCenter.contactDateOfBirth,
                    email = buyingCenter.contactEmail,
                    phone_number = buyingCenter.contactPhoneNumber,
                    id_number = buyingCenter.contactIdNumber
                )
            )
        )
    }

    // Public query methods
    fun getBuyingCentersByHubId(hubId: Int): Flow<List<BuyingCenterEntity>> =
        buyingCenterDao.getBuyingCentersByHubId(hubId)

    fun getAllBuyingCenters(): Flow<List<BuyingCenterEntity>> =
        buyingCenterDao.getAllBuyingCenters()

    suspend fun getBuyingCenterById(id: Int): BuyingCenterEntity? = withContext(Dispatchers.IO) {
        buyingCenterDao.getBuyingCenterById(id)
    }
}