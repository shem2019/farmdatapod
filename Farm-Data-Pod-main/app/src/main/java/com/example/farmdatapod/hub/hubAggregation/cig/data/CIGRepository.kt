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

    private suspend fun syncCigToServer(request: CIGCreateRequest, localId: Int): Result<CIGServerResponse?> {
        return try {
            // This call now correctly uses the right CIGCreateRequest model
            val response = apiService.registerCIG(request)
            if (response.isSuccessful) {
                val serverResponse = response.body()
                serverResponse?.let {
                    cigDao.updateSyncStatus(localId, it.id)
                }
                Result.success(serverResponse)
            } else {
                val errorMsg = "API Error ${response.code()}: ${response.errorBody()?.string()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
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
            val members: List<MemberRequest> = localCig.membersJson?.let { json ->
                val type = object : TypeToken<List<MemberRequest>>() {}.type
                gson.fromJson(json, type)
            } ?: emptyList()

            val request = CIGCreateRequest(
                cigName = localCig.cigName, hub = localCig.hub, numberOfMembers = localCig.numberOfMembers,
                dateEstablished = localCig.dateEstablished, constitution = localCig.constitution ?: "No",
                registration = localCig.registration ?: "No", certificate = localCig.certificate ?: "No",
                membershipRegister = localCig.membershipRegister, electionsHeld = localCig.electionsHeld ?: "No",
                dateOfLastElections = localCig.dateOfLastElections, meetingVenue = localCig.meetingVenue,
                frequency = localCig.frequency, scheduledMeetingDay = localCig.scheduledMeetingDay,
                scheduledMeetingTime = localCig.scheduledMeetingTime, members = members
            )

            val result = syncCigToServer(request, localCig.id)
            if (result.isSuccess) successCount++ else failureCount++
        }
        Result.success(SyncStats(successCount, failureCount, 0, failureCount == 0))
    }

    override suspend fun syncFromServer(): Result<Int> {
        return Result.success(0)
    }

    override suspend fun performFullSync(): Result<SyncStats> {
        return syncUnsynced()
    }

    fun getAllCIGs(): Flow<List<CIG>> = cigDao.getAllCIGs().flowOn(Dispatchers.IO)
}