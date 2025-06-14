package com.example.farmdatapod

import android.app.Application
import android.util.Log
import com.example.farmdatapod.sync.SyncManager
import com.example.farmdatapod.utils.SharedPrefs // Import SharedPrefs
import com.example.farmdatapod.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class
FarmDataPodApplication : Application() {
    lateinit var syncManager: SyncManager
    lateinit var tokenManager: TokenManager

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
            // Step 1: Initialize SharedPrefs
            val sharedPrefs = SharedPrefs(applicationContext) //

            // Step 2: Initialize TokenManager with the SharedPrefs instance
            tokenManager = TokenManager(sharedPrefs)

            // Initialize SyncManager as it depends on Context (its constructor was not changed)
            syncManager = SyncManager(applicationContext)

            setupSyncIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }

    private fun setupSyncIfNeeded() {
        // This will now correctly use the initialized tokenManager and its isTokenValid() method
        if (tokenManager.isTokenValid()) { //
            Log.d(TAG, "User is logged in, setting up sync") //
            syncManager.setupPeriodicSync() //
        } else {
            Log.d(TAG, "No valid token found, sync not initialized on app start") //
        }
    }

    // Provide getters if managers are private (existing comments)
    // fun getSyncManager() = syncManager
    // fun getTokenManager() = tokenManager
}