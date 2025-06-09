package com.example.farmdatapod.hub.hubAggregation.cig.data

import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.CIGCreateRequest
import com.example.farmdatapod.models.CIGServerResponse
import com.example.farmdatapod.models.MemberRequest
import com.example.farmdatapod.network.ApiService
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class CIGRepository(private val context: Context) : SyncableRepository {
    private val TAG = "CIGRepository"
    private val cigDao = AppDatabase.getInstance(context).cigDao()

    // THIS IS THE FIX:
    // We now call the getApiService method that only takes 'context',
    // exactly as it is defined in your RestClient.kt file.
    private val apiService: ApiService = RestClient.getApiService(context)

    private val gson = Gson()

    suspend fun saveCIGLocally(cig: CIG): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val localId = cigDao.insertCIG(cig.copy(syncStatus = false))
            Result.success(localId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CIG to local database", e)
            Result.failure(e)
        }
    }

    suspend fun syncSingleCigById(localId: Int): Result<CIGServerResponse> {
        val localCig = cigDao.getCIGById(localId).first()
        if (localCig == null) {
            return Result.failure(Exception("Could not find locally saved CIG with ID $localId to sync."))
        }

        val request = createApiRequestFromEntity(localCig)

        val jsonRequest = gson.toJson(request)
        Log.d(TAG, "--> Attempting to sync CIG (Local ID: $localId). Sending JSON:\n$jsonRequest")

        return try {
            val response = apiService.registerCIG(request)
            if (response.isSuccessful && response.body() != null) {
                val serverResponse = response.body()!!
                Log.i(TAG, "<-- SUCCESS: Synced CIG (Local ID: $localId). Server gave back ID: ${serverResponse.id}")
                cigDao.updateSyncStatus(localId, serverResponse.id)
                Result.success(serverResponse)
            } else {
                val errorMsg = "API Error ${response.code()}: ${response.errorBody()?.string()}"
                Log.e(TAG, "<-- FAILURE: Syncing CIG (Local ID: $localId). $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "<-- FAILURE: Exception while syncing CIG (Local ID: $localId)", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        val unsyncedCIGs = cigDao.getUnsyncedCIGs().first()
        if (unsyncedCIGs.isEmpty()) {
            return@withContext Result.success(SyncStats(0, 0, 0, true))
        }
        var successCount = 0
        var failureCount = 0
        for (localCig in unsyncedCIGs) {
            val result = syncSingleCigById(localCig.id)
            if (result.isSuccess) successCount++ else failureCount++
        }
        Result.success(SyncStats(successCount, failureCount, 0, failureCount == 0))
    }

    private fun createApiRequestFromEntity(localCig: CIG): CIGCreateRequest {
        val memberList: List<MemberRequest> = localCig.membersJson?.let { json ->
            val type = object : TypeToken<List<MemberRequest>>() {}.type
            gson.fromJson(json, type)
        } ?: emptyList()

        return CIGCreateRequest(
            cigName = localCig.cigName, hub = localCig.hub, numberOfMembers = localCig.numberOfMembers,
            dateEstablished = localCig.dateEstablished, constitution = localCig.constitution ?: "No",
            registration = localCig.registration ?: "No", certificate = localCig.certificate ?: "No",
            membershipRegister = localCig.membershipRegister, electionsHeld = localCig.electionsHeld ?: "No",
            dateOfLastElections = localCig.dateOfLastElections, meetingVenue = localCig.meetingVenue,
            frequency = localCig.frequency, scheduledMeetingDay = localCig.scheduledMeetingDay,
            scheduledMeetingTime = localCig.scheduledMeetingTime, members = memberList
        )
    }

    override suspend fun performFullSync(): Result<SyncStats> = syncUnsynced()
    override suspend fun syncFromServer(): Result<Int> = Result.success(0)
    fun getAllCIGs(): Flow<List<CIG>> = cigDao.getAllCIGs().flowOn(Dispatchers.IO)
}