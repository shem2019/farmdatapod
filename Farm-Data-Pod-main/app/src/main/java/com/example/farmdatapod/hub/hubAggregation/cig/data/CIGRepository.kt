package com.example.farmdatapod.hub.hubAggregation.cig.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.CIGRegistrationItem
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class CIGRepository(private val context: Context) : SyncableRepository {
    private val TAG = "CIGRepository"
    private val cigDao = AppDatabase.getInstance(context).cigDao()
    private val apiService = RestClient.getApiService(context)

    // Save CIG with optional sync
    suspend fun saveCIG(cig: CIG, isOnline: Boolean): Result<CIG> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save CIG: ${cig.cigName}, Online mode: $isOnline")

            // First save locally
            val localId = cigDao.insertCIG(cig)
            Log.d(TAG, "CIG saved locally with ID: $localId")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val cigRequest = createCIGRequest(cig)
                    val response = apiService.registerCig(cigRequest).execute()

                    if (response.isSuccessful) {
                        response.body()?.let { serverCIG ->
                            cigDao.updateSyncStatus(localId.toInt(), serverCIG.id)
                            Log.d(TAG, "CIG synced successfully with server ID: ${serverCIG.id}")
                        }
                    } else {
                        Log.e(TAG, "Server sync failed: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - CIG will sync later")
            }
            Result.success(cig)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CIG", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedCIGs = cigDao.getUnsyncedCIGs().first()
            Log.d(TAG, "Found ${unsyncedCIGs.size} unsynced CIGs")

            var successCount = 0
            var failureCount = 0

            unsyncedCIGs.forEach { cig ->
                try {
                    val cigRequest = createCIGRequest(cig)
                    val response = apiService.registerCig(cigRequest).execute()

                    if (response.isSuccessful) {
                        response.body()?.let { serverCIG ->
                            cigDao.updateSyncStatus(cig.id, serverCIG.id)
                            successCount++
                            Log.d(TAG, "Successfully synced ${cig.cigName}")
                        }
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to sync ${cig.cigName}: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${cig.cigName}", e)
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
            Log.d(TAG, "Starting GET request to fetch CIGs")
            val response = apiService.getCigs("Bearer ${getAuthToken()}").execute()

            when (response.code()) {
                200 -> {
                    val serverCIGs = response.body()?.string()?.let {
                        parseServerResponse(it)
                    }

                    if (serverCIGs == null) {
                        Log.d(TAG, "Server returned empty response")
                        return@withContext Result.success(0)
                    }

                    var savedCount = 0
                    serverCIGs.forEach { serverCIG ->
                        try {
                            // Get the first member from the list or use empty values
                            val member = serverCIG.members.firstOrNull() ?: mapOf<String, String>()

                            val localCIG = CIG(
                                serverId = serverCIG.id,
                                cigName = serverCIG.cig_name,
                                hub = serverCIG.hub,
                                numberOfMembers = serverCIG.no_of_members,
                                dateEstablished = serverCIG.date_established,
                                constitution = serverCIG.constitution,
                                registration = serverCIG.registration,
                                electionsHeld = serverCIG.elections_held,
                                dateOfLastElections = serverCIG.date_of_last_elections,
                                meetingVenue = serverCIG.meeting_venue,
                                frequency = serverCIG.frequency,
                                scheduledMeetingDay = serverCIG.scheduled_meeting_day,
                                scheduledMeetingTime = serverCIG.scheduled_meeting_time,
                                userId = serverCIG.user_id,
                                syncStatus = true,
                                // Member fields from the first member in the list
                                memberOtherName = member["other_name"] ?: "",
                                memberLastName = member["last_name"] ?: "",
                                memberGender = member["gender"] ?: "",
                                memberDateOfBirth = member["date_of_birth"] ?: "",
                                memberEmail = member["email"] ?: "",
                                memberPhoneNumber = member["phone_number"]?.toLongOrNull() ?: 0L,
                                memberIdNumber = member["id_number"]?.toIntOrNull() ?: 0,
                                productInvolved = member["product_involved"] ?: "",
                                hectorageRegisteredUnderCig = member["hectorage_registered_under_cig"] ?: "",
                                cigId = serverCIG.id
                            )
                            cigDao.insertCIG(localCIG)
                            savedCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving CIG ${serverCIG.cig_name}", e)
                        }
                    }
                    Result.success(savedCount)
                }
                401, 403 -> {
                    Log.e(TAG, "Authentication error: ${response.code()}")
                    Result.failure(Exception("Authentication error"))
                }
                else -> {
                    Log.e(TAG, "Unexpected response: ${response.code()}")
                    Result.failure(Exception("Server error"))
                }
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

    private fun createCIGRequest(cig: CIG) = CIGRegistrationItem(
        cig_name = cig.cigName,
        hub = cig.hub,
        no_of_members = cig.numberOfMembers,
        date_established = cig.dateEstablished,
        constitution = cig.constitution ?: "",  // Handle nullable field
        registration = cig.registration ?: "",  // Handle nullable field
        elections_held = cig.electionsHeld ?: "", // Handle nullable field
        date_of_last_elections = cig.dateOfLastElections,
        meeting_venue = cig.meetingVenue,
        frequency = cig.frequency,
        scheduled_meeting_day = cig.scheduledMeetingDay,
        scheduled_meeting_time = cig.scheduledMeetingTime,
        user_id = cig.userId ?: "",            // Handle nullable field
        id = cig.serverId ?: 0,                // Use serverId or default to 0
        members = listOf(                      // Create member map from CIG member fields
            mapOf(
                "other_name" to cig.memberOtherName,
                "last_name" to cig.memberLastName,
                "gender" to cig.memberGender,
                "date_of_birth" to cig.memberDateOfBirth,
                "email" to cig.memberEmail,
                "phone_number" to cig.memberPhoneNumber.toString(),
                "id_number" to cig.memberIdNumber.toString(),
                "product_involved" to cig.productInvolved,
                "hectorage_registered_under_cig" to cig.hectorageRegisteredUnderCig
            )
        )
    )

    fun getAllCIGs(): Flow<List<CIG>> = cigDao.getAllCIGs()

    fun getCIGById(id: Int): Flow<CIG?> = cigDao.getCIGById(id)
        .flowOn(Dispatchers.IO)
    private fun getAuthToken(): String? {
        // Implement your auth token retrieval logic here
        return null
    }

    private fun parseServerResponse(responseBody: String): List<CIGRegistrationItem> {
        // Implement your JSON parsing logic here
        return emptyList()
    }
}