package com.example.farmdatapod

import android.app.Application
import android.util.Log
import com.example.farmdatapod.sync.SyncManager
import com.example.farmdatapod.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FarmDataPodApplication : Application() {
    private lateinit var syncManager: SyncManager
    private lateinit var tokenManager: TokenManager

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "FarmDataPodApplication"
        private lateinit var instance: FarmDataPodApplication

        fun getInstance(): FarmDataPodApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            initializeManagers()
            setupSyncIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }

    private fun initializeManagers() {
        Log.d(TAG, "Initializing managers")
        syncManager = SyncManager(this)
        tokenManager = TokenManager(this)
    }

    private fun setupSyncIfNeeded() {
        if (tokenManager.isTokenValid()) {
            Log.d(TAG, "User is logged in, setting up sync")
            syncManager.setupPeriodicSync()
        } else {
            Log.d(TAG, "No valid token found, sync not initialized")
        }
    }

    fun getSyncManager() = syncManager
    fun getTokenManager() = tokenManager
}
