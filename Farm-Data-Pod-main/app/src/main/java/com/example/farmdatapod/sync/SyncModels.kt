package com.example.farmdatapod.sync



data class EntitySyncResult(
    val entityName: String,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val failures: Int = 0,
    val successful: Boolean = false,
    val error: String? = null
)

data class SyncResult(
    val results: List<EntitySyncResult>,
    val error: String? = null
) {
    fun isSuccessful(): Boolean = error == null && results.all { it.successful }
}

data class SyncStats(
    val uploadedCount: Int,
    val uploadFailures: Int,
    val downloadedCount: Int,
    val successful: Boolean
)

interface SyncableRepository {
    suspend fun performFullSync(): Result<SyncStats>
    suspend fun syncUnsynced(): Result<SyncStats>
    suspend fun syncFromServer(): Result<Int>
}