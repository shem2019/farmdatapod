package com.example.farmdatapod.hub.hubRegistration.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.HubResponse
import com.example.farmdatapod.models.RegisterRequest
import com.example.farmdatapod.models.RegisterResponse
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

class HubRepository(private val context: Context) : SyncableRepository {
    private val TAG = "HubRepository"
    private val hubDao = AppDatabase.getInstance(context).hubDao()




    suspend fun saveHub(hub: Hub, isOnline: Boolean): Result<Hub> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save hub: ${hub.hubName}, Online mode: $isOnline")

            // Check if the hub already exists based on hubName and hubCode
            val existingHub = hubDao.getHubByNameAndCode(hub.hubName, hub.hubCode)
            val localId: Long
            if (existingHub == null) {
                localId = hubDao.insertHub(hub)
                Log.d(TAG, "Hub saved locally with ID: $localId")
            } else {
                localId = existingHub.id.toLong()
                Log.d(TAG, "Hub already exists locally with ID: ${existingHub.id}")
                updateHubAndMarkForSync(existingHub)
            }

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val registerRequest = createRegisterRequest(hub)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).registerHubs(registerRequest)
                            .enqueue(object : Callback<RegisterResponse> {
                                override fun onResponse(
                                    call: Call<RegisterResponse>,
                                    response: Response<RegisterResponse>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverHub ->
                                            // Switch to a coroutine context to call markAsSynced
                                            CoroutineScope(Dispatchers.IO).launch {
                                                hubDao.markAsSynced(localId.toInt(), serverHub.id)
                                                Log.d(TAG, "Hub synced with server ID: ${serverHub.id}")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Hub will sync later")
            }
            Result.success(hub)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving hub", e)
            Result.failure(e)
        }
    }

    suspend fun updateHubAndMarkForSync(hub: Hub) {
        updateHub(hub)
        markForSync(hub.id)
    }

    suspend fun markForSync(localId: Int) = withContext(Dispatchers.IO) {
        hubDao.markForSync(localId)
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedHubs = hubDao.getUnsyncedHubs()
            Log.d(TAG, "Found ${unsyncedHubs.size} unsynced hubs")

            var successCount = 0
            var failureCount = 0

            unsyncedHubs.forEach { hub ->
                try {
                    val registerRequest = createRegisterRequest(hub)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).registerHubs(registerRequest)
                            .enqueue(object : Callback<RegisterResponse> {
                                override fun onResponse(
                                    call: Call<RegisterResponse>,
                                    response: Response<RegisterResponse>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverHub ->
                                            // Switch to a coroutine context to call markAsSynced
                                            CoroutineScope(Dispatchers.IO).launch {
                                                hubDao.markAsSynced(hub.id, serverHub.id)
                                                successCount++
                                                Log.d(TAG, "Successfully synced ${hub.hubName}")
                                            }
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync ${hub.hubName}")
                                    }
                                }

                                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing ${hub.hubName}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${hub.hubName}", e)
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
            var savedCount = 0

            // Clean duplicates first
            AppDatabase.getInstance(context).runInTransaction {
                launch {
                    try {
                        val duplicates = hubDao.findDuplicateHubs()
                        if (duplicates.isNotEmpty()) {
                            val deletedCount = hubDao.deleteDuplicateHubs()
                            Log.d(TAG, "Cleaned $deletedCount duplicate records")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cleaning duplicates", e)
                    }
                }
            }

            return@withContext suspendCancellableCoroutine { continuation ->
                context?.let { ctx ->
                    RestClient.getApiService(ctx).getHubs(null).enqueue(object : Callback<HubResponse> {
                        override fun onResponse(call: Call<HubResponse>, response: Response<HubResponse>) {
                            if (response.isSuccessful) {
                                try {
                                    val hubResponse = response.body()

                                    // Process in IO context
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            AppDatabase.getInstance(context).runInTransaction {
                                                hubResponse?.forms?.distinctBy { "${it.hubName}:${it.hubCode}" }
                                                    ?.forEach { serverHub ->
                                                        try {
                                                            // Launch a new coroutine for each processServerHub call
                                                            launch {
                                                                processServerHub(serverHub)?.let { savedCount++ }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(TAG, "Error processing hub ${serverHub.hubName}", e)
                                                        }
                                                    }
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
                                when (response.code()) {
                                    401, 403 -> {
                                        continuation.resume(Result.failure(SecurityException("Authentication error: ${response.code()}")))
                                    }
                                    else -> {
                                        continuation.resume(Result.failure(RuntimeException("Unexpected response: ${response.code()}")))
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<HubResponse>, t: Throwable) {
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


    private suspend fun processServerHub(serverHub: com.example.farmdatapod.models.Hub): Hub? {
        val existingHub = hubDao.getHubByNameAndCode(serverHub.hubName, serverHub.hubCode)

        return when {
            existingHub == null -> {
                val localHub = createLocalHubFromServer(serverHub)
                hubDao.insertHub(localHub)
                Log.d(TAG, "Inserted new hub: ${serverHub.hubName}")
                localHub
            }
            shouldUpdate(existingHub, serverHub) -> {
                val updatedHub = createUpdatedHub(existingHub, serverHub)
                hubDao.updateHub(updatedHub)
                Log.d(TAG, "Updated existing hub: ${serverHub.hubName}")
                updatedHub
            }
            else -> {
                Log.d(TAG, "Hub ${serverHub.hubName} is up to date")
                null
            }
        }
    }

    private fun createLocalHubFromServer(serverHub: com.example.farmdatapod.models.Hub): Hub {
        return Hub(
            serverId = serverHub.id,
            region = serverHub.region,
            hubName = serverHub.hubName,
            hubCode = serverHub.hubCode,
            address = serverHub.address,
            yearEstablished = serverHub.yearEstablished,
            ownership = serverHub.ownership,
            floorSize = serverHub.floorSize,
            facilities = serverHub.facilities,
            inputCenter = serverHub.inputCenter,
            typeOfBuilding = serverHub.typeOfBuilding,
            longitude = serverHub.longitude,
            latitude = serverHub.latitude,
            userId = serverHub.userId,
            syncStatus = true,
            contactOtherName = serverHub.keyContacts.firstOrNull()?.otherName ?: "",
            contactLastName = serverHub.keyContacts.firstOrNull()?.lastName ?: "",
            contactGender = serverHub.keyContacts.firstOrNull()?.gender ?: "",
            contactRole = serverHub.keyContacts.firstOrNull()?.role ?: "",
            contactDateOfBirth = serverHub.keyContacts.firstOrNull()?.dateOfBirth ?: "",
            contactEmail = serverHub.keyContacts.firstOrNull()?.email ?: "",
            contactPhoneNumber = serverHub.keyContacts.firstOrNull()?.phoneNumber ?: "",
            contactIdNumber = serverHub.keyContacts.firstOrNull()?.idNumber ?: 0,
            hubId = serverHub.keyContacts.firstOrNull()?.hubId,
            buyingCenterId = serverHub.keyContacts.firstOrNull()?.buyingCenterId,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun createUpdatedHub(existingHub: Hub, serverHub: com.example.farmdatapod.models.Hub): Hub {
        return existingHub.copy(
            serverId = serverHub.id,
            region = serverHub.region,
            hubName = serverHub.hubName,
            hubCode = serverHub.hubCode,
            address = serverHub.address,
            yearEstablished = serverHub.yearEstablished,
            ownership = serverHub.ownership,
            floorSize = serverHub.floorSize,
            facilities = serverHub.facilities,
            inputCenter = serverHub.inputCenter,
            typeOfBuilding = serverHub.typeOfBuilding,
            longitude = serverHub.longitude,
            latitude = serverHub.latitude,
            userId = serverHub.userId,
            syncStatus = true,
            contactOtherName = serverHub.keyContacts.firstOrNull()?.otherName ?: existingHub.contactOtherName,
            contactLastName = serverHub.keyContacts.firstOrNull()?.lastName ?: existingHub.contactLastName,
            contactGender = serverHub.keyContacts.firstOrNull()?.gender ?: existingHub.contactGender,
            contactRole = serverHub.keyContacts.firstOrNull()?.role ?: existingHub.contactRole,
            contactDateOfBirth = serverHub.keyContacts.firstOrNull()?.dateOfBirth ?: existingHub.contactDateOfBirth,
            contactEmail = serverHub.keyContacts.firstOrNull()?.email ?: existingHub.contactEmail,
            contactPhoneNumber = serverHub.keyContacts.firstOrNull()?.phoneNumber ?: existingHub.contactPhoneNumber,
            contactIdNumber = serverHub.keyContacts.firstOrNull()?.idNumber ?: existingHub.contactIdNumber,
            hubId = serverHub.keyContacts.firstOrNull()?.hubId ?: existingHub.hubId,
            buyingCenterId = serverHub.keyContacts.firstOrNull()?.buyingCenterId ?: existingHub.buyingCenterId,
            lastModified = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )
    }
    private fun shouldUpdate(existingHub: Hub, serverHub: com.example.farmdatapod.models.Hub): Boolean {
        // Update if server ID is different or if any of the fields have changed
        return existingHub.serverId != serverHub.id ||
                existingHub.region != serverHub.region ||
                existingHub.address != serverHub.address ||
                existingHub.yearEstablished != serverHub.yearEstablished ||
                existingHub.ownership != serverHub.ownership ||
                existingHub.floorSize != serverHub.floorSize ||
                existingHub.facilities != serverHub.facilities ||
                existingHub.inputCenter != serverHub.inputCenter ||
                existingHub.typeOfBuilding != serverHub.typeOfBuilding ||
                existingHub.longitude != serverHub.longitude ||
                existingHub.latitude != serverHub.latitude ||
                existingHub.contactOtherName != (serverHub.keyContacts.firstOrNull()?.otherName ?: "")
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

    private fun createRegisterRequest(hub: Hub) = RegisterRequest(
        region = hub.region,
        hub_name = hub.hubName,
        hub_code = hub.hubCode,
        address = hub.address,
        year_established = hub.yearEstablished,
        ownership = hub.ownership,
        floor_size = hub.floorSize,
        facilities = hub.facilities,
        input_center = hub.inputCenter,
        type_of_building = hub.typeOfBuilding,
        longitude = hub.longitude,
        latitude = hub.latitude,
        key_contacts = listOf(
            com.example.farmdatapod.models.KeyContact(
                other_name = hub.contactOtherName,
                last_name = hub.contactLastName,
                gender = hub.contactGender,
                role = hub.contactRole,
                date_of_birth = hub.contactDateOfBirth,
                email = hub.contactEmail,
                phone_number = hub.contactPhoneNumber,
                id_number = hub.contactIdNumber,
            )
        )
    )




    fun getAllHubs(): Flow<List<Hub>> = hubDao.getAllHubs()

    suspend fun getHubById(id: Int): Hub? = withContext(Dispatchers.IO) {
        hubDao.getHubById(id)
    }

    suspend fun deleteHub(hub: Hub) = withContext(Dispatchers.IO) {
        hubDao.deleteHub(hub)
    }

    suspend fun updateHub(hub: Hub) = withContext(Dispatchers.IO) {
        hubDao.updateHub(hub)
    }
}