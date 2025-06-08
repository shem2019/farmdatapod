package com.example.farmdatapod

import android.app.Application
import android.util.Log
import com.example.farmdatapod.sync.SyncManager
import com.example.farmdatapod.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FarmDataPodApplication : Application() {
    lateinit var syncManager: SyncManager // Keep as is or make private and provide getter
    lateinit var tokenManager: TokenManager // Make public or provide getter

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
            // Initialize managers ONCE here
            tokenManager = TokenManager(applicationContext) // Initialize here
            syncManager = SyncManager(applicationContext)   // Initialize here

            setupSyncIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }

    private fun setupSyncIfNeeded() {
        // Now this will use the correctly initialized tokenManager
        if (tokenManager.isTokenValid()) { //
            Log.d(TAG, "User is logged in, setting up sync")
            syncManager.setupPeriodicSync()
        } else {
            Log.d(TAG, "No valid token found, sync not initialized on app start")
        }
    }

    // Provide getters if managers are private
    // fun getSyncManager() = syncManager
    // fun getTokenManager() = tokenManager
}